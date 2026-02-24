(ns demo.alice
  "RLS Demo: Alice - The Project Owner

   Alice owns Project Alpha. She has full control within her domain:
   - Sees all tasks in her project
   - Can modify her project and grant access
   - Cannot see or modify Bob's Project Beta"
  (:require
    [demo.core :as core]
    [demo.data :as data]))

(defn step-1-projects
  "Alice checks her projects.
   EXPECTED: Only Project Alpha visible (she owns it)."
  []
  (println)
  (println "═══════════════════════════════════════════════════════════════")
  (println "  ALICE: Step 1 - Checking project visibility")
  (println "═══════════════════════════════════════════════════════════════")
  (println)
  (println "  Alice owns Project Alpha.")
  (println "  She is NOT an owner or member of Project Beta.")
  (println)
  (println "  EXPECTED: Only Project Alpha visible")
  (println)
  (core/demo-projects))

(defn step-2-tasks
  "Alice sees ALL tasks in her project.
   EXPECTED: All 3 Alpha tasks (owner sees everything)."
  []
  (println)
  (println "═══════════════════════════════════════════════════════════════")
  (println "  ALICE: Step 2 - Checking tasks (owner sees everything)")
  (println "═══════════════════════════════════════════════════════════════")
  (println)
  (println "  As project owner, Alice sees ALL tasks in Project Alpha,")
  (println "  regardless of who they're assigned to.")
  (println)
  (println "  This includes:")
  (println "    - Alpha Task 1 (assigned to Alice herself)")
  (println "    - Alpha Task 2 (assigned to Charlie)")
  (println "    - Alpha Task 3 (assigned to Engineering group)")
  (println)
  (println "  EXPECTED: All 3 Alpha tasks visible")
  (println)
  (core/demo-tasks))

(defn step-3-cross-project-isolation
  "Alice cannot see Bob's project.
   EXPECTED: No Beta project or tasks visible."
  []
  (println)
  (println "═══════════════════════════════════════════════════════════════")
  (println "  ALICE: Step 3 - Cross-project isolation")
  (println "═══════════════════════════════════════════════════════════════")
  (println)
  (println "  Even though Alice is a project owner, she ONLY has access")
  (println "  to her own projects. She cannot see Project Beta or its tasks.")
  (println)
  (println "  EXPECTED: No Beta project or tasks visible")
  (println)
  {:projects (core/demo-projects)
   :tasks (core/demo-tasks)})

(defn step-4-grant-access
  "Alice can add members to her project.
   EXPECTED: Success - owner can modify their own project."
  []
  (println)
  (println "═══════════════════════════════════════════════════════════════")
  (println "  ALICE: Step 4 - Grant access (add project member)")
  (println "═══════════════════════════════════════════════════════════════")
  (println)
  (println "  As owner, Alice can modify her project's members list.")
  (println "  This grants project-level access to other users.")
  (println)
  (println "  Adding Charlie as a member of Project Alpha...")
  (println)
  (println "  EXPECTED: Success")
  (println)
  (core/mutate
    [{:mutation :stackProjectList
      :variables {:data [{:euuid data/project-alpha-euuid
                          :members [{:euuid data/charlie-euuid}]}]}
      :types {:data :ProjectInput}
      :selection {:euuid nil
                  :name nil
                  :members [{:selections {:name nil}}]}}]))


(comment
  ;; MANDATORY STEP!
  (do
    (require '[eywa.client])
    (eywa.client/open-pipe))

  (do
    (println)
    (println "╔═══════════════════════════════════════════════════════════════╗")
    (println "║                                                               ║")
    (println "║   ALICE: THE PROJECT OWNER                                    ║")
    (println "║                                                               ║")
    (println "║   Alice owns Project Alpha with full control over her         ║")
    (println "║   domain. She can see all tasks and grant access.             ║")
    (println "║                                                               ║")
    (println "║   Make sure you are logged in as alice/password               ║")
    (println "║                                                               ║")
    (println "╚═══════════════════════════════════════════════════════════════╝"))


  (core/mutate
    [{:mutation :stackProjectList
      :variables {:data [{:euuid data/project-alpha-euuid
                          :name "Project Alpha"}]}
      :types {:data :ProjectInput}
      :selection {:euuid nil
                  :name nil}}])

  (step-1-projects)
  (step-2-tasks)
  (step-3-cross-project-isolation)
  (step-4-grant-access))
