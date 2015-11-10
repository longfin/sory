(ns attendance-check.db.core
  (:require [clojure.string :as s]
            [monger.core :as mg]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [environ.core :refer [env]])
  (:import org.bson.types.ObjectId))


(defn serialize [coll]
  (clojure.walk/postwalk
   #(if (= (type %) ObjectId) (str %) %) coll))


;; Tries to get the Mongo URI from the environment variable
(defonce db (let [uri (:database-url env)
                  {:keys [db]} (mg/connect-via-uri uri)]
              db))


(defmacro defcrud
  "Helper macro for define entity.
  it generates CRUD function that manage entity having given properties.

  Usage:

  ```
  (defcrud \"book\" :title :author)

  ;; generates

  (defn create-book [arg])
  (defn get-book [arg])
  (defn get-book-by-id [id])
  (defn update-book [id arg])
  (defn delete-book-by-id [id])
  ```
  "

  [entity & props]
  (let [create-fn-sym (symbol (str "create-" entity))
        get-fn-sym (symbol (str "get-" entity))
        get-by-id-fn-sym (symbol (str "get-" entity "-by-id"))
        update-fn-sym (symbol (str "update-" entity))
        props-syms (map #(symbol (name %)) props)
        props-map (apply hash-map (interleave
                                   (map #(keyword (s/replace (name %) "-" "_")) props)
                                   props-syms))
        delete-by-id-fn-sym (symbol (str "delete-" entity "-by-id"))]
    `(do
       (defn ~create-fn-sym [arg#]
         (let [sanitized# (select-keys arg# ~(vec props))]
           (mc/insert-and-return db ~entity sanitized#)))
       (defn ~get-fn-sym [arg#]
         (mc/find-maps db ~entity arg#))
       (defn ~get-by-id-fn-sym [id#]
         (mc/find-one-as-map db ~entity {:_id (ObjectId. id#)}))
       (defn ~update-fn-sym [id# arg#]
         (let [sanitized# (select-keys arg# ~(vec props))
               id# (if (instance? ObjectId id#)
                     id#
                     (ObjectId. id#))]
          (mc/update db ~entity {:_id id#} {$set sanitized#})))
       (defn ~delete-by-id-fn-sym [id#]
         (mc/remove db ~entity {:_id (ObjectId. id#)}))
       nil)))


(defcrud "tutor" :name :email :password)
(defcrud "student" :name :email :password)
(defcrud "course" :name :tutor :students)
(defcrud "class" :course :tutor :students)
(defcrud "attendance-check" :course :code)
