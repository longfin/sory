(ns chat.socket)

(def ^:dynamic *socket*)

(defmacro with-socket [socket & body]
  `(binding [*socket* ~socket]
     ~@body))
