(ns demo.diana
  "RLS Demo: Diana - The Outsider

   Diana is the KEY demo case. She's a consultant with an allocation record,
   but she has NO access to the underlying task via RLS guards.

   This demonstrates cross-dataset RLS protection."
  (:require
    [demo.core :as core]))

(defn step-1-projects
  "Diana tries to see projects.
   EXPECTED: Empty - Diana is not a project owner or member."
  []
  (println)
  (println "═══════════════════════════════════════════════════════════════")
  (println "  DIANA: Step 1 - Attempting to view projects")
  (println "═══════════════════════════════════════════════════════════════")
  (println)
  (println "  Diana is not a project owner and not a member of any project.")
  (println "  EXPECTED: Empty result")
  (println)
  (core/demo-projects))

(defn step-2-tasks
  "Diana tries to see tasks.
   EXPECTED: Empty - Diana is not assigned to any tasks."
  []
  (println)
  (println "═══════════════════════════════════════════════════════════════")
  (println "  DIANA: Step 2 - Attempting to view tasks")
  (println "═══════════════════════════════════════════════════════════════")
  (println)
  (println "  Diana is not assigned to any tasks (not in assignee or")
  (println "  assignee_group).")
  (println "  EXPECTED: Empty result")
  (println)
  (core/demo-tasks))

(defn step-3-allocations
  "Diana checks her allocations - THE KEY MOMENT.
   EXPECTED: Allocation visible BUT :user_task is NULL!"
  []
  (println)
  (println "═══════════════════════════════════════════════════════════════")
  (println "  DIANA: Step 3 - Checking allocations")
  (println "  ★★★ THIS IS THE KEY RLS DEMONSTRATION ★★★")
  (println "═══════════════════════════════════════════════════════════════")
  (println)
  (println "  Diana has an allocation record (hours: 10, for Alpha Task 1).")
  (println "  She can see her OWN allocation data...")
  (println)
  (println "  BUT the :user_task field should be NULL!")
  (println)
  (println "  Why? Diana is NOT:")
  (println "    - Owner of Project Alpha (Alice owns it)")
  (println "    - Assignee of Alpha Task 1 (Alice is assigned)")
  (println "    - Member of any group assigned to the task")
  (println)
  (println "  The Task RLS guards block her access to task details,")
  (println "  even though she has an allocation linking to that task.")
  (println)
  (println "  THIS PROVES RLS WORKS ACROSS DATASET BOUNDARIES!")
  (println)
  (println "  EXPECTED: [{:hours 10, :user_task nil, ...}]")
  (println)
  (core/demo-allocations))


(comment
  (do
    (require '[eywa.client])
    (eywa.client/open-pipe))

  (do
    (println)
    (println "╔═══════════════════════════════════════════════════════════════╗")
    (println "║                                                               ║")
    (println "║   DIANA: THE OUTSIDER                                         ║")
    (println "║                                                               ║")
    (println "║   Diana is a consultant with an allocation but NO task        ║")
    (println "║   access. This is the key RLS demonstration.                  ║")
    (println "║                                                               ║")
    (println "║   Make sure you are logged in as diana/password               ║")
    (println "║                                                               ║")
    (println "╚═══════════════════════════════════════════════════════════════╝"))



  (step-1-projects)
  (step-2-tasks)
  (step-3-allocations))
