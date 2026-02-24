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
    [clojure.core.async :as async]
    [demo.data :as data]
    [eywa.client :as eywa]
    [rogue.graphql :as graphql]))


(def project-management-euuid #uuid "6cf45a62-6872-497f-873c-ee88671435d0")
(def resource-planning-euuid #uuid "86f794a4-44bc-4cf4-bfbc-4908df9e3a47")



(defn load-dataset
  "Load dataset JSON from resources"
  [filename]
  (slurp (str "resources/" filename)))


(defn mutate
  "Execute GraphQL mutations using rogue format"
  [mutations]
  (let [{:keys [query variables]} (graphql/mutations mutations)
        result (async/<!! (eywa/graphql query variables))]
    (if (:errors result)
      (throw (ex-info "GraphQL mutation error" {:errors (:errors result)}))
      result)))


(defn query
  "Execute GraphQL queries using rogue format"
  [queries]
  (let [result (async/<!! (eywa/graphql (graphql/queries queries)))]
    (if (:errors result)
      (throw (ex-info "GraphQL query error" {:errors (:errors result)}))
      result)))


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




;;; ============================================================================
;;; RLS Demo Queries
;;; ============================================================================
;;
;; Login as different users to test RLS:
;;   alice/password, bob/password, charlie/password, diana/password
;;
;; Run these queries after logging in as each user to see RLS in action.


;; -----------------------------------------------------------------------------
;; DEMO: Search Projects
;; -----------------------------------------------------------------------------
;; LOGIN AS ALICE → EXPECTED: Sees Project Alpha only (she owns it)
;; LOGIN AS BOB   → EXPECTED: Sees Project Beta only (he owns it)
;; LOGIN AS CHARLIE → EXPECTED: Sees both? (if member relation works)
;; LOGIN AS DIANA → EXPECTED: Sees nothing (no project access)

(defn demo-projects []
  (query [{:query :searchProject
           :selection {:euuid nil
                       :name nil
                       :project_owner [{:selections {:name nil}}]}}]))


;; -----------------------------------------------------------------------------
;; DEMO: Search Tasks
;; -----------------------------------------------------------------------------
;; LOGIN AS ALICE → EXPECTED: All Alpha tasks (as owner)
;; LOGIN AS BOB   → EXPECTED: All Beta tasks (as owner)
;; LOGIN AS CHARLIE → EXPECTED: Alpha Task 2, Beta Task 2 (assigned to him)
;; LOGIN AS DIANA → EXPECTED: Nothing (no task access)

(defn demo-tasks []
  (query [{:query :searchProjectTask
           :selection {:title nil
                       :assignee [{:selections {:name nil}}]
                       :assignee_group [{:selections {:name nil}}]
                       :project [{:selections {:name nil}}]}}]))


;; -----------------------------------------------------------------------------
;; DEMO: Search Allocations - THE KEY DEMO CASE
;; -----------------------------------------------------------------------------
;; LOGIN AS CHARLIE → EXPECTED: His 2 allocations WITH full task details
;;
;; LOGIN AS DIANA → EXPECTED: Her allocation (hours: 10)
;;                  BUT user_task should be NULL!
;;                  Diana has allocation to Alpha Task 1, but she's NOT:
;;                  - Owner of Project Alpha
;;                  - Assignee of Alpha Task 1
;;                  So Task guards block her access to task details.
;;                  THIS PROVES RLS WORKS ACROSS DATASETS!

(defn demo-allocations []
  (query [{:query :searchAllocation
           :selection {:hours nil
                       :notes nil
                       :user [{:selections {:name nil}}]
                       :user_task [{:selections {:title nil
                                                 :project [{:selections {:name nil}}]}}]}}]))


(defn -main [& _]
  (eywa/open-pipe))


(comment

  ;; ===========================================================================
  ;; RLS DEMO SCENARIO
  ;; ===========================================================================
  ;;
  ;; This demo shows Row-Level Security (RLS) in action across two datasets:
  ;;   - Project Management: Projects, Tasks (with RLS guards)
  ;;   - Resource Planning: Allocations (links to Tasks from another dataset)
  ;;
  ;; Users:
  ;;   - alice/password   - Owner of Project Alpha
  ;;   - bob/password     - Owner of Project Beta
  ;;   - charlie/password - Developer, member of Engineering group
  ;;   - diana/password   - Consultant with allocation but NO task access
  ;;
  ;; RLS Guard Paths demonstrated:
  ;;   - Direct reference:  project_owner → User
  ;;   - N:N relation:      members → User
  ;;   - Group membership:  assignee_group → UserGroup → users → User (multihop)
  ;;   - Cross-dataset:     Allocation → Task (different datasets)
  ;;
  ;; ===========================================================================


  ;; ===========================================================================
  ;; ACT 1: SETUP (Login as admin/superuser)
  ;; ===========================================================================
  ;; First, we set up the demo data as an admin user who bypasses RLS.

  (eywa/open-pipe)

  ;; Import the datasets (if not already deployed)
  (import-dataset (load-dataset "Project_Management_0_1_4.json"))
  (import-dataset (load-dataset "Resource_Planning_0_1_0.json"))

  ;; Create demo users with passwords
  (mutate [{:mutation :stackUserList
            :variables {:data data/users}
            :types {:data :UserInput}
            :selection {:euuid nil
                        :name nil}}])

  ;; Create Engineering group (empty for now - we'll add Charlie later)
  (mutate [{:mutation :stackUserGroupList
            :variables {:data [{:euuid data/engineering-euuid
                                :name "Engineering"
                                :active true}]}
            :types {:data :UserGroupInput}
            :selection {:euuid nil
                        :name nil}}])

  ;; Create projects owned by Alice and Bob
  (mutate [{:mutation :stackProjectList
            :variables {:data data/projects}
            :types {:data :ProjectInput}
            :selection {:euuid nil
                        :name nil}}])

  ;; Create tasks - NOTE: Alpha Task 3 has NO assignee yet (group-assigned later)
  (mutate [{:mutation :stackProjectTaskList
            :variables {:data [(assoc data/alpha-task-1 :assignee {:euuid data/alice-euuid})
                               (assoc data/alpha-task-2 :assignee {:euuid data/charlie-euuid})
                               (dissoc data/alpha-task-3 :assignee_group)
                               (assoc data/beta-task-1 :assignee {:euuid data/bob-euuid})
                               (assoc data/beta-task-2 :assignee {:euuid data/charlie-euuid})]}
            :types {:data :ProjectTaskInput}
            :selection {:euuid nil
                        :title nil}}])

  ;; Create allocations - including Diana's KEY DEMO CASE
  (mutate [{:mutation :stackAllocationList
            :variables {:data data/allocations}
            :types {:data :AllocationInput}
            :selection {:euuid nil
                        :hours nil}}])

  ;; Verify setup - should see all data as superuser
  (demo-projects)
  (demo-tasks)


  ;; ===========================================================================
  ;; ACT 2: THE OUTSIDER (Login as diana/password)
  ;; ===========================================================================
  ;; Diana is a consultant. She has an allocation record linking her to a task,
  ;; but she's not assigned to that task and doesn't own the project.
  ;; RLS should block her from seeing task details.

  ;; Diana tries to see projects
  (demo-projects)
  ;; => {:searchProject nil}
  ;; RESULT: Empty. Diana owns no projects and is not a member of any.

  ;; Diana tries to see tasks
  (demo-tasks)
  ;; => {:searchProjectTask nil}
  ;; RESULT: Empty. Diana is not assigned to any tasks.

  ;; Diana checks her allocations - THE KEY MOMENT
  (demo-allocations)
  ;; => {:searchAllocation [{:hours 10,
  ;;                         :notes "Consulting - Diana on Alpha",
  ;;                         :user {:name "diana"},
  ;;                         :user_task nil}]}    <-- TASK IS NULL!
  ;;
  ;; RESULT: Diana sees her allocation EXISTS (hours, notes visible),
  ;; but :user_task is NULL because Task RLS guards block her access!
  ;;
  ;; THIS PROVES RLS WORKS ACROSS DATASET BOUNDARIES!


  ;; ===========================================================================
  ;; ACT 3: THE DEVELOPER (Login as charlie/password)
  ;; ===========================================================================
  ;; Charlie is assigned to some tasks but doesn't own any projects.
  ;; He can see his assigned tasks but not projects or unassigned tasks.

  ;; Charlie checks projects
  (demo-projects)
  ;; => {:searchProject nil}
  ;; RESULT: Empty. Charlie is not a project owner or member (yet).

  ;; Charlie checks tasks - only sees tasks assigned directly to him
  (demo-tasks)
  ;; => [{:title "Alpha Task 2", :assignee {:name "charlie"}, :project nil},
  ;;     {:title "Beta Task 2", :assignee {:name "charlie"}, :project nil}]
  ;;
  ;; NOTE: Charlie does NOT see Alpha Task 3 (no assignee, no group yet)
  ;; NOTE: :project is nil because Project RLS blocks him

  ;; Charlie tries to add himself as project member - BLOCKED!
  (mutate [{:mutation :syncProjectList
            :variables {:data [{:euuid data/project-alpha-euuid
                                :members [{:euuid data/charlie-euuid}]}]}
            :types {:data :ProjectInput}
            :selection {:euuid nil}}])
  ;; => THROWS: "Write access denied by RLS"
  ;; Charlie cannot modify projects he doesn't own!


  ;; ===========================================================================
  ;; ACT 4: THE OWNER GRANTS ACCESS (Login as alice/password)
  ;; ===========================================================================
  ;; Alice owns Project Alpha. She can grant access to others.

  ;; Alice sees her project and all tasks
  (demo-projects)
  ;; => [{:name "Project Alpha", :project_owner {:name "alice"}}]

  (demo-tasks)
  ;; => All 3 Alpha tasks visible (including Task 3 with no assignee)

  ;; Alice adds Charlie as project MEMBER
  (mutate [{:mutation :syncProjectList
            :variables {:data [{:euuid data/project-alpha-euuid
                                :members [{:euuid data/charlie-euuid}]}]}
            :types {:data :ProjectInput}
            :selection {:euuid nil
                        :name nil
                        :members [{:selections {:name nil}}]}}])
  ;; => SUCCESS! Charlie is now a member

  ;; Alice tries to modify Bob's project - BLOCKED!
  (mutate [{:mutation :syncProjectList
            :variables {:data [{:euuid data/project-beta-euuid
                                :description "Hacked!"}]}
            :types {:data :ProjectInput}
            :selection {:euuid nil}}])
  ;; => THROWS: "Write access denied by RLS"


  ;; ===========================================================================
  ;; ACT 5: MEMBER ACCESS (Login as charlie/password)
  ;; ===========================================================================
  ;; Charlie was added as member - now he sees the project!

  ;; Charlie NOW sees Project Alpha via members relation
  (demo-projects)
  ;; => [{:name "Project Alpha", :project_owner {:name "alice"}}]

  ;; Tasks now show project details for Alpha
  (demo-tasks)
  ;; => [{:title "Alpha Task 2", :project {:name "Project Alpha"}},  <-- VISIBLE!
  ;;     {:title "Beta Task 2", :project nil}]  <-- Still blocked
  ;;
  ;; NOTE: Still doesn't see Alpha Task 3 (not assigned to him or his group)


  ;; ===========================================================================
  ;; ACT 6: GROUP-BASED ACCESS (Login as admin/superuser)
  ;; ===========================================================================
  ;; Now we demonstrate MULTIHOP access: Task → assignee_group → users
  ;; We'll add Charlie to Engineering group, then assign Task 3 to that group.

  ;; Step 1: Add Charlie to Engineering group
  (mutate [{:mutation :syncUserGroupList
            :variables {:data [{:euuid data/engineering-euuid
                                :users [{:euuid data/charlie-euuid}]}]}
            :types {:data :UserGroupInput}
            :selection {:name nil
                        :users [{:selections {:name nil}}]}}])
  ;; => {:name "Engineering", :users [{:name "charlie"}]}

  ;; Step 2: Assign Alpha Task 3 to Engineering group
  (mutate [{:mutation :syncProjectTaskList
            :variables {:data [{:euuid data/alpha-task-3-euuid
                                :assignee_group {:euuid data/engineering-euuid}}]}
            :types {:data :ProjectTaskInput}
            :selection {:title nil
                        :assignee_group [{:selections {:name nil}}]}}])
  ;; => {:title "Alpha Task 3 - Code Review", :assignee_group {:name "Engineering"}}


  ;; ===========================================================================
  ;; ACT 7: MULTIHOP IN ACTION (Login as charlie/password)
  ;; ===========================================================================
  ;; Charlie should now see Alpha Task 3 via group membership!
  ;; Path: Task.assignee_group → UserGroup.users → User (charlie)

  ;; Charlie now sees ALL Alpha tasks
  (demo-tasks)
  ;; => [{:title "Alpha Task 2", :assignee {:name "charlie"}, ...},
  ;;     {:title "Alpha Task 3 - Code Review", :assignee nil,
  ;;      :assignee_group {:name "Engineering"}, ...},    <-- NOW VISIBLE!
  ;;     {:title "Beta Task 2", ...}]
  ;;
  ;; Charlie sees Task 3 because:
  ;;   Task.assignee_group = Engineering
  ;;   Engineering.users contains Charlie
  ;;   RLS guard follows the multihop path!


  ;; ===========================================================================
  ;; ACT 8: DIANA STILL BLOCKED (Login as diana/password)
  ;; ===========================================================================
  ;; Diana is NOT in Engineering group - she still can't see Task 3

  (demo-tasks)
  ;; => {:searchProjectTask nil}
  ;; Diana still sees nothing - group membership doesn't help her


  ;; ===========================================================================
  ;; SUMMARY
  ;; ===========================================================================
  ;;
  ;; RLS Guard Types Demonstrated:
  ;;
  ;; 1. DIRECT REFERENCE (project_owner → User)
  ;;    - Alice sees Project Alpha because she owns it
  ;;
  ;; 2. N:N RELATION (members → User)
  ;;    - Charlie sees Project Alpha after being added as member
  ;;
  ;; 3. MULTIHOP / GROUP-BASED (assignee_group → UserGroup → users → User)
  ;;    - Charlie sees Alpha Task 3 via Engineering group membership
  ;;    - Diana still blocked (not in the group)
  ;;
  ;; 4. CROSS-DATASET (Allocation → Task)
  ;;    - Diana's allocation exists but :user_task is null
  ;;    - Task RLS applies even when accessed from another dataset
  ;;
  ;; 5. WRITE PROTECTION
  ;;    - Charlie blocked from modifying projects
  ;;    - Alice blocked from modifying Bob's project
  ;;
  ;; 6. DYNAMIC ACCESS
  ;;    - Adding user to group/members instantly grants access
  ;;    - No code changes, no redeployment - just data!
  ;;
  ;; ===========================================================================
  )
