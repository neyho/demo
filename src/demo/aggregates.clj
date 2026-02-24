(ns demo.aggregates
  "EYWA Aggregates Demo - Showcasing _agg queries and aliases.

   This demo uses 100 allocations across 5 tasks and 8 users to demonstrate
   GraphQL aggregate functions: sum, avg, min, max, count, and field aliases."
  (:require
    [clojure.edn :as edn]
    [demo.core :as core]))


(defn load-data
  "Load allocations data from EDN file"
  []
  (edn/read-string (slurp "resources/allocations.edn")))


(defn setup!
  "Setup demo data - run as superuser"
  []
  (let [{:keys [users allocations]} (load-data)]
    ;; Create/update users
    (core/mutate [{:mutation :stackUserList
                   :variables {:data users}
                   :types {:data :UserInput}
                   :selection {:euuid nil
                               :name nil}}])
    ;; Create allocations
    (core/mutate [{:mutation :stackAllocationList
                   :variables {:data (mapv (fn [a]
                                             {:euuid (:euuid a)
                                              :hours (:hours a)
                                              :notes (:notes a)
                                              :user {:euuid (:user a)}
                                              :user_task {:euuid (:user_task a)}})
                                           allocations)}
                   :types {:data :AllocationInput}
                   :selection {:euuid nil}}])
    :done))


;;; ============================================================================
;;; Aggregate Queries
;;; ============================================================================

(defn task-hours
  "Get hours aggregated per task (max, min, avg, sum)"
  []
  (core/query
    [{:query :searchProjectTask
      :selection {:title nil
                  :_count [{:selections {:allocations nil}}
                           {:selections
                            {:allocations
                             [{:args {:_where {:user {:name {:_eq "charlie"}}}}
                               :alias :charlies_allocations}]}}]
                  :_agg [{:selections
                          {:allocations
                           [{:selections
                             {:hours [{:selections
                                       {:max nil
                                        :min nil
                                        :avg nil
                                        :sum nil}}]}}]}}]}}]))


(defn task-hours-by-user
  "Get hours per task, broken down by user using aliases"
  []
  (core/query
    [{:query :searchProjectTask
      :selection {:title nil
                  :_agg [{:selections
                          {:allocations
                           [{:alias "charlie"
                             :args {:_where {:user {:name {:_eq "charlie"}}}}
                             :selections
                             {:hours [{:selections {:sum nil
                                                    :avg nil}}]}}
                            {:alias "alice"
                             :args {:_where {:user {:name {:_eq "alice"}}}}
                             :selections
                             {:hours [{:selections {:sum nil
                                                    :avg nil}}]}}
                            {:alias "bob"
                             :args {:_where {:user {:name {:_eq "bob"}}}}
                             :selections
                             {:hours [{:selections {:sum nil
                                                    :avg nil}}]}}
                            {:alias "total"
                             :selections
                             {:hours [{:selections {:sum nil
                                                    :avg nil}}]}}]}}]}}]))


(defn project-hours
  "Get hours aggregated per project through tasks"
  []
  (core/query
    [{:query :searchProject
      :selection {:name nil
                  :tasks [{:selections
                           {:title nil
                            :_agg [{:selections
                                    {:allocations
                                     [{:selections
                                       {:hours [{:selections
                                                 {:sum nil
                                                  :avg nil}}]}}]}}]}}]}}]))


(defn user-report
  "Get a report for a specific user - hours per task with counts"
  [username]
  (core/query
    [{:query :searchProjectTask
      :selection {:title nil
                  :project [{:selections {:name nil}}]
                  :allocations [{:alias :user_allocations
                                 :args {:_where {:user {:name {:_eq username}}}}
                                 :selections {:hours nil :notes nil}}]
                  :_count [{:selections
                            {:allocations [{:args {:_where {:user {:name {:_eq username}}}}
                                            :alias :allocation_count}]}}]
                  :_agg [{:selections
                          {:allocations
                           [{:alias :user_hours
                             :args {:_where {:user {:name {:_eq username}}}}
                             :selections
                             {:hours [{:selections {:sum nil :avg nil}}]}}]}}]}}]))


(comment
  ;; ===========================================================================
  ;; AGGREGATES DEMO SCENARIO
  ;; ===========================================================================
  ;;
  ;; This demo showcases EYWA's aggregate query capabilities:
  ;;   - sum, avg, min, max (no underscore prefix)
  ;;   - _agg field on entities for nested aggregates
  ;;   - Aliases for multiple aggregates in one query
  ;;   - _where filters within aggregates
  ;;
  ;; ===========================================================================


  ;; ===========================================================================
  ;; SETUP (Login as admin/superuser)
  ;; ===========================================================================

  (core/open-pipe)

  ;; Load 100 allocations across 8 users and 5 tasks
  (setup!)
  ;; => :done


  ;; ===========================================================================
  ;; BASIC AGGREGATES - Per Task
  ;; ===========================================================================

  (task-hours)
  ;; => {:searchProjectTask
  ;;     [{:title "Alpha Task 1 - Setup Infrastructure"
  ;;       :_agg {:allocations {:hours {:max 32 :min 2 :avg 10.76 :sum 226}}}}
  ;;      {:title "Alpha Task 2 - Implement Feature X"
  ;;       :_agg {:allocations {:hours {:max 40 :min 2 :avg 16.09 :sum 338}}}}
  ;;      {:title "Alpha Task 3 - Code Review"
  ;;       :_agg {:allocations {:hours {:max 8 :min 2 :avg 4.5 :sum 90}}}}
  ;;      ...]}


  ;; ===========================================================================
  ;; ALIASED AGGREGATES - Multiple breakdowns in one query
  ;; ===========================================================================
  ;;
  ;; The KILLER FEATURE: Get multiple aggregates with different filters
  ;; in a SINGLE query using aliases!

  (task-hours-by-user)
  ;; => {:searchProjectTask
  ;;     [{:title "Alpha Task 1 - Setup Infrastructure"
  ;;       :_agg {:charlie {:hours {:sum 56 :avg 14.0}}
  ;;              :alice {:hours {:sum 54 :avg 10.8}}
  ;;              :bob {:hours {:sum 0 :avg nil}}
  ;;              :total {:hours {:sum 226 :avg 10.76}}}}
  ;;      ...]}
  ;;
  ;; Each task shows:
  ;;   - Charlie's hours on this task
  ;;   - Alice's hours on this task
  ;;   - Bob's hours on this task
  ;;   - Total hours from all users
  ;;
  ;; All in ONE query! No N+1 problem.


  ;; ===========================================================================
  ;; NESTED AGGREGATES - Project → Tasks → Allocations
  ;; ===========================================================================

  (project-hours)
  ;; => {:searchProject
  ;;     [{:name "Project Alpha"
  ;;       :tasks [{:title "Alpha Task 3 - Code Review"
  ;;                :_agg {:allocations {:hours {:sum 90 :avg 4.5}}}}
  ;;               {:title "Alpha Task 1 - Setup Infrastructure"
  ;;                :_agg {:allocations {:hours {:sum 226 :avg 10.76}}}}
  ;;               {:title "Alpha Task 2 - Implement Feature X"
  ;;                :_agg {:allocations {:hours {:sum 338 :avg 16.09}}}}]}
  ;;      {:name "Project Beta"
  ;;       :tasks [{:title "Beta Task 2 - Build Prototype"
  ;;                :_agg {:allocations {:hours {:sum 275 :avg 13.09}}}}
  ;;               {:title "Beta Task 1 - Design System"
  ;;                :_agg {:allocations {:hours {:sum 186 :avg 9.3}}}}]}]}
  ;;
  ;; Project-level view with task breakdowns - perfect for dashboards!


  ;; ===========================================================================
  ;; USER REPORT - Parameterized query for specific user
  ;; ===========================================================================

  (user-report "charlie")
  ;; => {:searchProjectTask
  ;;     [{:title "Alpha Task 2 - Implement Feature X"
  ;;       :project {:name "Project Alpha"}
  ;;       :user_allocations [{:hours 20 :notes "Sprint 1 - Charlie on Alpha"}
  ;;                          {:hours 40 :notes "Week 1 - Core module"} ...]
  ;;       :_count {:allocation_count 7}
  ;;       :_agg {:user_hours {:hours {:sum 150 :avg 21.4}}}}
  ;;      {:title "Beta Task 2 - Build Prototype"
  ;;       :_count {:allocation_count 7}
  ;;       :_agg {:user_hours {:hours {:sum 137 :avg 19.6}}}}
  ;;      ...]}
  ;;
  ;; Full breakdown for one user: their allocations, count, and hour totals.


  ;; ===========================================================================
  ;; RLS + AGGREGATES - The Real Proof
  ;; ===========================================================================
  ;;
  ;; CRITICAL: Aggregates respect RLS guards!
  ;; Each user only sees/aggregates data they have access to.
  ;;
  ;; Users:
  ;;   - alice/password   → Owns Project Alpha (3 tasks)
  ;;   - bob/password     → Owns Project Beta (2 tasks)
  ;;   - charlie/password → Assigned to some tasks
  ;;   - diana/password   → No task access (allocation exists but blocked)
  ;;
  ;; ===========================================================================


  ;; ===========================================================================
  ;; ACT 1: SUPERUSER BASELINE (Login as admin)
  ;; ===========================================================================

  (core/open-pipe)

  (task-hours)
  ;; => 5 tasks visible with all aggregate data
  ;; Superuser bypasses RLS - sees everything


  ;; ===========================================================================
  ;; ACT 2: PROJECT OWNER - ALICE (Login as alice/password)
  ;; ===========================================================================
  ;;
  ;; Alice owns Project Alpha. She should ONLY see Alpha task aggregates.

  (core/open-pipe)

  (task-hours)
  ;; => {:searchProjectTask
  ;;     [{:title "Alpha Task 1 - Setup Infrastructure"
  ;;       :_count {:allocations 21 :charlies_allocations 4}
  ;;       :_agg {:allocations {:hours {:sum 226 ...}}}}
  ;;      {:title "Alpha Task 2 - Implement Feature X" ...}
  ;;      {:title "Alpha Task 3 - Code Review" ...}]}
  ;;
  ;; RESULT: Only 3 Alpha tasks! No Beta tasks visible.
  ;; Aggregates computed ONLY on Alice's accessible data.

  (project-hours)
  ;; => {:searchProject [{:name "Project Alpha" :tasks [...]}]}
  ;;
  ;; Only Project Alpha visible with its task aggregates.


  ;; ===========================================================================
  ;; ACT 3: PROJECT OWNER - BOB (Login as bob/password)
  ;; ===========================================================================
  ;;
  ;; Bob owns Project Beta. He should ONLY see Beta task aggregates.

  (core/open-pipe)

  (task-hours)
  ;; => {:searchProjectTask
  ;;     [{:title "Beta Task 1 - Design System"
  ;;       :_agg {:allocations {:hours {:sum 186 ...}}}}
  ;;      {:title "Beta Task 2 - Build Prototype"
  ;;       :_agg {:allocations {:hours {:sum 275 ...}}}}]}
  ;;
  ;; RESULT: Only 2 Beta tasks! No Alpha tasks visible.

  (project-hours)
  ;; => {:searchProject [{:name "Project Beta" :tasks [...]}]}


  ;; ===========================================================================
  ;; ACT 4: DEVELOPER - CHARLIE (Login as charlie/password)
  ;; ===========================================================================
  ;;
  ;; Charlie is assigned to specific tasks (Alpha Task 2, Beta Task 2).
  ;; He should only see those tasks' aggregates.

  (core/open-pipe)

  (task-hours)
  ;; => {:searchProjectTask
  ;;     [{:title "Alpha Task 2 - Implement Feature X" ...}
  ;;      {:title "Beta Task 2 - Build Prototype" ...}]}
  ;;
  ;; RESULT: Only his 2 assigned tasks! Not the other 3.

  (user-report "charlie")
  ;; => Charlie's hours breakdown, but ONLY for tasks he can access


  ;; ===========================================================================
  ;; ACT 5: OUTSIDER - DIANA (Login as diana/password)
  ;; ===========================================================================
  ;;
  ;; Diana has an allocation record but NO task access.
  ;; She should see ZERO task aggregates.

  (core/open-pipe)

  (task-hours)
  ;; => {:searchProjectTask []}
  ;;
  ;; RESULT: Empty! Diana cannot see any tasks.
  ;; Even though her allocation EXISTS in the database,
  ;; the Task RLS guards block her from seeing task data.

  (project-hours)
  ;; => {:searchProject []}
  ;;
  ;; No projects visible either.


  ;; ===========================================================================
  ;; SUMMARY - What This Proves
  ;; ===========================================================================
  ;;
  ;; 1. AGGREGATES RESPECT RLS
  ;;    - Alice's sum/avg/count only includes Alpha data
  ;;    - Bob's sum/avg/count only includes Beta data
  ;;    - Diana sees nothing (aggregates can't leak blocked data)
  ;;
  ;; 2. NO DATA LEAKAGE
  ;;    - Aggregate functions (sum, avg, min, max) only compute
  ;;      over rows the user is authorized to see
  ;;    - _count only counts accessible relations
  ;;
  ;; 3. SECURE BY DEFAULT
  ;;    - No special handling needed in queries
  ;;    - RLS guards automatically filter before aggregation
  ;;    - Same query, different results per user
  ;;
  ;; ===========================================================================


  ;; ===========================================================================
  ;; KEY FEATURES DEMONSTRATED
  ;; ===========================================================================
  ;;
  ;; 1. _agg field - aggregates on related entities
  ;;    {:_agg {:allocations {:hours {:sum nil}}}}
  ;;
  ;; 2. _count field - count relations (with aliases + filters)
  ;;    {:_count {:allocations nil :charlies_allocations {...}}}
  ;;
  ;; 3. Aggregate functions - sum, avg, min, max
  ;;    (no underscore prefix)
  ;;
  ;; 4. Aliases - multiple aggregates with different filters in ONE query
  ;;    {:alias "charlie" :args {:_where {...}} :selections {...}}
  ;;
  ;; 5. Nested aggregates - through relation chains
  ;;    Project → tasks → _agg → allocations → hours
  ;;
  ;; 6. Filtering within aggregates
  ;;    :args {:_where {:user {:name {:_eq "charlie"}}}}
  ;;
  ;; 7. RLS integration - aggregates automatically respect guards
  ;;    No special code needed, secure by default
  ;;
  ;; ===========================================================================
  )
