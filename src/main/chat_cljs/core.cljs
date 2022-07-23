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


(js/console.log "message")

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
    (println server)
    (.on server "connection" (fn [socket] 
                               (info "A user connected...")
                               (.emit (.-broadcast socket) "A new user joined!")
                               (.on socket "chat message" (fn [msg]
                                                            (js/console.log (clj->js {:message msg :created-at (js/Date)}))
                                                            (info (str "A message received: " msg)) 
                                                            (.emit server "chat message" (clj->js {:text msg :created-at (js/Date)}))))))))

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
