(ns chat-cljs.core
  (:require ["express" :as express]
            [path]
            [http :as h]
            ["socket.io" :refer [Server]] 
            [lambdaisland.glogi :as log]
            [lambdaisland.glogi.console :as glogi-console]))
(enable-console-print!)
(glogi-console/install!)

(defonce server (atom nil))

(defn info [message]
  (log/info :message message))

(defn to-js [map]
  (clj->js map))

; public directory of assets
(def public
  (path/resolve js/__dirname "public"))

; base routing defs for chat
(def chat-router
  (let [router (express/Router)]
    (.get router "/" (fn [req res]
                        (.sendFile res (str public "/index.html"))))))

(def app
  (let [express (express)]
    (.get express "/" (fn [req res]
                    (info (str "request => " req))
                    (.send res "Hello ClojureScript")))
    ; for chat app
    (.use express "/chat" chat-router)))

(def http
  (h/Server app))

; callback def of emittion on connection from user
(def io
  (let [server (new Server http)]
    (.on server
         "connection"
         (fn [socket] 
           (info "A user connected...") 
           (.emit (.-broadcast socket) "A new user joined!") 
           (.on socket
                "chat message" (fn [msg] (let [user (.-user msg)
                                               text (.-text msg) 
                                               created-at (js/Date)]
                                           (js/console.log msg)
                                           (info (str user " sent a message: " msg)) 
                                           (.emit server "chat message" (to-js {:text text :created-at created-at})))))))))

(defn start! []
  ; evaluate callbacks on connection
  io 
  (reset! server (.listen http 3000 (fn [] (info "Express listen on port 3000...")))))

(defn stop! []
  (.close @server (fn [] (info "Stopping server..."))))

(defn restart! []
  (.close @server start!))

(set! *main-cli-fn* start!)

(defn main []
  (start!))
