(ns demo.charlie
  "RLS Demo: Charlie - The Team Member

   Charlie is a developer who demonstrates:
   - Task assignment-based access
   - Group membership access (Engineering group)
   - Contrast with Diana (he CAN see task details in allocations)"
  (:require
    [demo.core :as core]))

(defn step-1-projects
  "Charlie checks project visibility.
   EXPECTED: Both projects (member of both)."
  []
  (println)
  (println "═══════════════════════════════════════════════════════════════")
  (println "  CHARLIE: Step 1 - Checking project visibility")
  (println "═══════════════════════════════════════════════════════════════")
  (println)
  (println "  Charlie is a member of both Project Alpha and Project Beta")
  (println "  (via the members relation).")
  (println "  EXPECTED: Both projects visible")
  (println)
  (core/demo-projects))

(defn step-2-tasks
  "Charlie checks his assigned tasks.
   EXPECTED: Alpha Task 2, Alpha Task 3 (via group), Beta Task 2."
  []
  (println)
  (println "═══════════════════════════════════════════════════════════════")
  (println "  CHARLIE: Step 2 - Checking assigned tasks")
  (println "═══════════════════════════════════════════════════════════════")
  (println)
  (println "  Charlie is assigned to:")
  (println "    - Alpha Task 2 (direct assignment)")
  (println "    - Alpha Task 3 (via Engineering group)")
  (println "    - Beta Task 2 (direct assignment)")
  (println)
  (println "  EXPECTED: Alpha Task 2, Alpha Task 3, Beta Task 2")
  (println)
  (core/demo-tasks))

(defn step-3-allocations
  "Charlie checks allocations - contrast with Diana.
   EXPECTED: Allocations WITH full :user_task details visible."
  []
  (println)
  (println "═══════════════════════════════════════════════════════════════")
  (println "  CHARLIE: Step 3 - Checking allocations")
  (println "  (Contrast this with Diana's view!)")
  (println "═══════════════════════════════════════════════════════════════")
  (println)
  (println "  Charlie has allocations linking to tasks he's assigned to.")
  (println "  Unlike Diana, Charlie CAN see the :user_task details because")
  (println "  he passes the Task RLS guards (he's the assignee).")
  (println)
  (println "  EXPECTED: Allocations WITH full task details visible")
  (println)
  (core/demo-allocations))

(defn step-4-group-access
  "Demonstrate group-based task access.
   EXPECTED: Alpha Task 3 visible via Engineering group membership."
  []
  (println)
  (println "═══════════════════════════════════════════════════════════════")
  (println "  CHARLIE: Step 4 - Group-based access (Engineering)")
  (println "═══════════════════════════════════════════════════════════════")
  (println)
  (println "  Alpha Task 3 is assigned to the 'Engineering' group.")
  (println "  Charlie is a member of the Engineering group.")
  (println)
  (println "  RLS follows the multihop path:")
  (println "    Task.assignee_group → UserGroup.users → User (charlie)")
  (println)
  (println "  EXPECTED: Alpha Task 3 visible via group membership")
  (println)
  (core/demo-tasks))


(comment
  (require '[eywa.client])
  (eywa.client/open-pipe)

  (do
    (println)
    (println "╔═══════════════════════════════════════════════════════════════╗")
    (println "║                                                               ║")
    (println "║   CHARLIE: THE TEAM MEMBER                                    ║")
    (println "║                                                               ║")
    (println "║   Charlie demonstrates assignment and group-based access.     ║")
    (println "║   Compare his allocation view with Diana's to see RLS.        ║")
    (println "║                                                               ║")
    (println "║   Make sure you are logged in as charlie/password             ║")
    (println "║                                                               ║")
    (println "╚═══════════════════════════════════════════════════════════════╝"))

  (core/mutate
    [{:mutation :stackProjectList
      :variables {:data [{:euuid demo.data/project-alpha-euuid
                          :name "Project Alpha"}]}
      :types {:data :ProjectInput}
      :selection {:euuid nil
                  :name nil}}])

  (step-1-projects)
  (step-2-tasks)
  (step-3-allocations)
  (step-4-group-access))
