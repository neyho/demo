(ns demo.data
  "Demo data with stable EUUIDs for RLS demonstration")

;;; ============================================================================
;;; Users
;;; ============================================================================

(def alice-euuid #uuid "a1111111-1111-1111-1111-111111111111")
(def bob-euuid #uuid "b2222222-2222-2222-2222-222222222222")
(def charlie-euuid #uuid "c3333333-3333-3333-3333-333333333333")
(def diana-euuid #uuid "d4444444-4444-4444-4444-444444444444")

(def alice {:euuid alice-euuid
            :name "alice"
            :active true
            :password "password"})
(def bob {:euuid bob-euuid
          :name "bob"
          :active true
          :password "password"})
(def charlie {:euuid charlie-euuid
              :name "charlie"
              :active true
              :password "password"})
(def diana {:euuid diana-euuid
            :name "diana"
            :active true
            :password "password"})

(def users [alice bob charlie diana])

;;; ============================================================================
;;; Groups
;;; ============================================================================

(def engineering-euuid #uuid "e5555555-5555-5555-5555-555555555555")

(def engineering
  {:euuid engineering-euuid
   :name "Engineering"
   :active true
   :users [{:euuid charlie-euuid}]})

(def groups [engineering])

;;; ============================================================================
;;; Projects
;;; ============================================================================

(def project-alpha-euuid #uuid "aa000000-0000-0000-0000-000000000001")
(def project-beta-euuid #uuid "bb000000-0000-0000-0000-000000000002")

(def project-alpha
  {:euuid project-alpha-euuid
   :name "Project Alpha"
   :description "Alice's flagship project"
   :active true
   :project_owner {:euuid alice-euuid}})

(def project-beta
  {:euuid project-beta-euuid
   :name "Project Beta"
   :description "Bob's innovation project"
   :active true
   :project_owner {:euuid bob-euuid}})

(def projects [project-alpha project-beta])

;;; ============================================================================
;;; Tasks
;;; ============================================================================

(def alpha-task-1-euuid #uuid "a1000000-0000-0000-0000-000000000001")
(def alpha-task-2-euuid #uuid "a1000000-0000-0000-0000-000000000002")
(def alpha-task-3-euuid #uuid "a1000000-0000-0000-0000-000000000003")
(def beta-task-1-euuid #uuid "b1000000-0000-0000-0000-000000000001")
(def beta-task-2-euuid #uuid "b1000000-0000-0000-0000-000000000002")

(def alpha-task-1
  {:euuid alpha-task-1-euuid
   :title "Alpha Task 1 - Setup Infrastructure"
   :description "Initial infrastructure setup"
   :status :In_Progress
   :assignee {:euuid alice-euuid}
   :project {:euuid project-alpha-euuid}})

(def alpha-task-2
  {:euuid alpha-task-2-euuid
   :title "Alpha Task 2 - Implement Feature X"
   :description "Core feature implementation"
   :status :ToDo
   :assignee {:euuid charlie-euuid}
   :project {:euuid project-alpha-euuid}})

(def alpha-task-3
  {:euuid alpha-task-3-euuid
   :title "Alpha Task 3 - Code Review"
   :description "Team code review"
   :status :ToDo
   :assignee_group {:euuid engineering-euuid}
   :project {:euuid project-alpha-euuid}})

(def beta-task-1
  {:euuid beta-task-1-euuid
   :title "Beta Task 1 - Design System"
   :description "Design the system architecture"
   :status :In_Progress
   :assignee {:euuid bob-euuid}
   :project {:euuid project-beta-euuid}})

(def beta-task-2
  {:euuid beta-task-2-euuid
   :title "Beta Task 2 - Build Prototype"
   :description "Build working prototype"
   :status :ToDo
   :assignee {:euuid charlie-euuid}
   :project {:euuid project-beta-euuid}})

(def tasks [alpha-task-1 alpha-task-2 alpha-task-3 beta-task-1 beta-task-2])

;;; ============================================================================
;;; Allocations (Resource Planning dataset)
;;; ============================================================================

(def alloc-1-euuid #uuid "11100000-0000-0000-0000-000000000001")
(def alloc-2-euuid #uuid "11100000-0000-0000-0000-000000000002")
(def alloc-3-euuid #uuid "11100000-0000-0000-0000-000000000003")

(def allocation-1
  {:euuid alloc-1-euuid
   :hours 20
   :notes "Sprint 1 - Charlie on Alpha"
   :user {:euuid charlie-euuid}
   :user_task {:euuid alpha-task-2-euuid}})

(def allocation-2
  {:euuid alloc-2-euuid
   :hours 15
   :notes "Sprint 1 - Charlie on Beta"
   :user {:euuid charlie-euuid}
   :user_task {:euuid beta-task-2-euuid}})

;; KEY DEMO CASE: Diana has allocation but NO access to the task via RLS
(def allocation-3
  {:euuid alloc-3-euuid
   :hours 10
   :notes "Consulting - Diana on Alpha (KEY DEMO CASE)"
   :user {:euuid diana-euuid}
   :user_task {:euuid alpha-task-1-euuid}})

(def allocations [allocation-1 allocation-2 allocation-3])
