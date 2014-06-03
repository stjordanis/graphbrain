(ns graphbrain.web.handlers.aichat
  (:require [graphbrain.web.common :as common]
            [clojure.data.json :as json]
            [graphbrain.db.gbdb :as gb]
            [graphbrain.db.graphjava :as gbj]
            [graphbrain.db.maps :as maps]
            [graphbrain.db.urlnode :as url]
            [graphbrain.braingenerators.pagereader :as pr]
            [graphbrain.string :as gbstr])
  (:import (com.graphbrain.eco Prog)))

(def prog
  (Prog/fromString
    (slurp "eco/chat.eco")
    (gbj/graph)))

(defn- aichat-reply
  [root-id vertex sentence]
  (let [goto-id (if (gb/edge? vertex)
                  (second (maps/ids vertex))
                  root-id)]
    (json/write-str {:sentence sentence
                     :newedges (list (:id vertex))
                     :gotoid goto-id})))

(defn- sentence-type
  [sentence]
  (if (and (gbstr/no-spaces? sentence)
           (or (.startsWith sentence "http://") (.startsWith sentence "https://")))
    :url :fact))

(defn- process-fact
  [user root sentence]
  (. prog setVertex "$user" (gbj/map->user-obj user))
  (. prog setVertex "$root" (gbj/map->vertex-obj root))
  (let
      [ctxts-list (. prog wv sentence 0)
       vertex (gbj/vertex-obj->map
               (. (first (. (first ctxts-list) getCtxts)) getTopRetVertex))]
    (if (gb/edge? vertex)
      (gb/putv! common/gbdb (assoc vertex :score 1) (:id user)))
    (aichat-reply (:id root) vertex (:id vertex))))

(defn process-url
  [user root sentence]
  (let [url-id (url/url->id sentence)]
    (pr/extract-knowledge! sentence)
    (aichat-reply url-id nil (str "processed url: " sentence))))

(defn handle-aichat
  [request]
  (let [sentence ((request :form-params) "sentence")
        root-id ((request :form-params) "rootId")
        root (if root-id (gb/getv common/gbdb root-id))
        user (common/get-user request)]
    (case (sentence-type sentence)
      :fact (process-fact user root sentence)
      :url (process-url user root sentence))))