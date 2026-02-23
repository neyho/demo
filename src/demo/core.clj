(ns demo.core
  "EYWA RLS Demo - REPL-driven demonstration of Row-Level Security
   across multiple datasets.

   Usage:
   1. Run via eywa: eywa run -c \"bb nrepl-server\"
   2. Connect and load: (require '[demo.core :as demo] :reload)
   3. Open pipe: (demo/open-pipe)
   4. Import dataset: (demo/import-dataset (demo/load-dataset \"...\"))
   5. Run queries and see RLS in action"
  (:require
    [demo.data :as data]
    [eywa.client :as eywa]
    [rogue.graphql :as graphql]))


(def project-management-euuid #uuid "6cf45a62-6872-497f-873c-ee88671435d0")
(def resource-planning-euuid #uuid "86f794a4-44bc-4cf4-bfbc-4908df9e3a47")


(defn open-pipe
  "Open the JSON-RPC pipe for communication with EYWA"
  []
  (eywa/open-pipe))


(defn load-dataset
  "Load dataset JSON from resources"
  [filename]
  (slurp (str "resources/" filename)))


(defn mutate
  "Execute GraphQL mutations using rogue format"
  [mutations]
  (let [{:keys [query variables]} (graphql/mutations mutations)]
    (eywa/graphql query variables)))


(defn query
  "Execute GraphQL queries using rogue format"
  [queries]
  (eywa/graphql (graphql/queries queries)))


(defn import-dataset
  [dataset]
  (mutate [{:mutation :importDataset
            :variables {:dataset dataset}
            :types {:dataset :Transit}
            :selection {:euuid nil
                        :name nil}}]))


(defn destroy-dataset
  [euuid]
  (mutate [{:mutation :destroyDataset
            :variables {:euuid euuid}
            :types {:euuid :UUID}
            :selection {:euuid nil}}]))


(defn delete-user
  [{:keys [name]}]
  (mutate
    [{:mutation :deleteUser
      :variables {:name name}
      :types {:name :String}}]))

(defn sync-users
  [users]
  (mutate
    [{:mutation :syncUserList
      :variables {:data users}
      :types {:data :UserInput}
      :selection {:euuid nil
                  :name nil}}]))


(defn -main [& _]
  (open-pipe))


(comment)
