(ns graphbrain.web.handlers.nodeactions
  (:use (graphbrain.web common)
        (ring.util response))
  (:require [graphbrain.db.gbdb :as gb]
            [graphbrain.db.maps :as maps]))

(defn- remove-vertex
  [request]
  (let
    [user (get-user request)
     edge-id ((request :form-params) "edge")]
    (gb/remove! gbdb
      (maps/id->edge edge-id)
      (:id user))))

(defn handle-nodeactions
  [request]
  (let
    [vert-id (:* (:route-params request))
     op ((request :form-params) "op")]
    (if (= op "remove")
      (remove-vertex request))
    (redirect (str "/node/" vert-id))))