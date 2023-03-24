(ns my-mount
  (:require [mount.core :as mount]))

(defn startx []
  (do (println "starting x")
      {:x :ok}))
(defn stopx []
  (do (println "stop x")
      (swap! evil-state dissoc :x)))

(mount/defstate statex
  :start (startx)
  :stop (stopx))




