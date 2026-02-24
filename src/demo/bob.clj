(ns demo.bob
  "RLS Demo: Bob - The Other Owner

   Bob owns Project Beta. This demonstrates:
   - Complete isolation from Alice's Project Alpha
   - Owner-level access within his own domain
   - Parallel ownership with no cross-contamination"
  (:require
    [demo.core :as core]
    [demo.data :as data]))

(defn step-1-projects
  "Bob checks his projects.
   EXPECTED: Only Project Beta visible."
  []
  (println)
  (println "═══════════════════════════════════════════════════════════════")
  (println "  BOB: Step 1 - Checking project visibility")
  (println "═══════════════════════════════════════════════════════════════")
  (println)
  (println "  Bob owns Project Beta.")
  (println "  He is NOT an owner or member of Project Alpha.")
  (println)
  (println "  EXPECTED: Only Project Beta visible")
  (println)
  (core/demo-projects))

(defn step-2-tasks
  "Bob sees only his project's tasks.
   EXPECTED: Only Beta tasks visible."
  []
  (println)
  (println "═══════════════════════════════════════════════════════════════")
  (println "  BOB: Step 2 - Checking tasks")
  (println "═══════════════════════════════════════════════════════════════")
  (println)
  (println "  As owner of Project Beta, Bob sees all Beta tasks:")
  (println "    - Beta Task 1 (assigned to Bob himself)")
  (println "    - Beta Task 2 (assigned to Charlie)")
  (println)
  (println "  Bob should NOT see any Alpha tasks.")
  (println)
  (println "  EXPECTED: Only Beta tasks visible")
  (println)
  (core/demo-tasks))

(defn step-3-isolation
  "Bob cannot see or modify Alice's project.
   EXPECTED: Mutation blocked by RLS write guards."
  []
  (println)
  (println "═══════════════════════════════════════════════════════════════")
  (println "  BOB: Step 3 - Isolation from Alice's project")
  (println "═══════════════════════════════════════════════════════════════")
  (println)
  (println "  Bob and Alice are peer owners with separate domains.")
  (println "  Neither can see or modify the other's projects.")
  (println)
  (println "  Attempting to modify Alice's Project Alpha...")
  (println)
  (println "  EXPECTED: Blocked by RLS write guards")
  (println)
  (core/mutate
    [{:mutation :syncProjectList
      :variables {:data [{:euuid data/project-alpha-euuid
                          :description "Hacked by Bob!"}]}
      :types {:data :ProjectInput}
      :selection {:euuid nil
                  :name nil
                  :description nil
                  :project_owner [{:selections {:name nil}}]
                  :members [{:selections {:name nil}}]}}]))


(comment
  ;; MANDATORY
  (do
    (require '[eywa.client])
    (eywa.client/open-pipe))

  (do
    (println)
    (println "╔═══════════════════════════════════════════════════════════════╗")
    (println "║                                                               ║")
    (println "║   BOB: THE OTHER OWNER                                        ║")
    (println "║                                                               ║")
    (println "║   Bob owns Project Beta, demonstrating complete isolation     ║")
    (println "║   between project owners.                                     ║")
    (println "║                                                               ║")
    (println "║   Make sure you are logged in as bob/password                 ║")
    (println "║                                                               ║")
    (println "╚═══════════════════════════════════════════════════════════════╝"))



  (step-1-projects)
  (step-2-tasks)
  (step-3-isolation))
