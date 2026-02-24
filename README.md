# Row-Level Security Demo

**One database. Four users. Four different views of the same data.**

This demo showcases EYWA's Row-Level Security (RLS) in action, demonstrating how data access is automatically filtered based on user identity - even across dataset boundaries.

---

## The Cast

| User | Role | Password | What They Can See |
|------|------|----------|-------------------|
| **Alice** | Project Owner | `password` | Everything in Project Alpha |
| **Bob** | Project Owner | `password` | Everything in Project Beta |
| **Charlie** | Team Member | `password` | Tasks assigned to him + group tasks |
| **Diana** | Consultant | `password` | Her allocation... but not the task it points to |

Diana is the **key demo case** - she proves RLS works across dataset boundaries.

---

## Quick Start

### Prerequisites

1. EYWA server running on `localhost:8080`
2. Datasets deployed: `Project_Management` and `Resource_Planning`
3. Demo data loaded (users, projects, tasks, allocations)

### Running User REPLs

Each user has their own REPL session in `repl/<user>/`:

```bash
# Terminal 1 - Alice
cd repl/alice && eywa run -c "bb nrepl-server"

# Terminal 2 - Diana
cd repl/diana && eywa run -c "bb nrepl-server"

# ... etc for bob, charlie
```

Connect your editor to the nREPL port shown, then:

```clojure
;; MANDATORY - establish connection
(require '[eywa.client])
(eywa.client/open-pipe)

;; Load the demo namespace
(require '[demo.diana :as diana] :reload)
```

---

## The Demo Story

### Act 1: Diana's Dilemma (The AHA Moment)

Diana is a consultant. She has an allocation record - 10 hours billed to a task. But she's not assigned to that task, doesn't own the project, and isn't in any group.

**Run as Diana:**

```clojure
(diana/step-1-projects)
;; => {:searchProject []}
;; Diana sees NO projects

(diana/step-2-tasks)
;; => {:searchProjectTask []}
;; Diana sees NO tasks

(diana/step-3-allocations)
;; => {:searchAllocation [{:hours 10,
;;                         :notes "Consulting - Diana on Alpha",
;;                         :user {:name "diana"},
;;                         :user_task nil}]}  ;; <-- THE KEY!
```

**The AHA moment:** Diana can see her allocation EXISTS (hours, notes visible), but `:user_task` is **null**. The Task entity has RLS guards, and Diana doesn't pass any of them.

**This proves RLS works across dataset boundaries.** The Allocation lives in `Resource_Planning`, but when it tries to resolve `user_task` (which points to `Project_Management`), the Task guards kick in and block access.

---

### Act 2: Charlie's Access (The Team Member)

Charlie is assigned to tasks and is a member of the Engineering group.

**Run as Charlie:**

```clojure
(charlie/step-1-projects)
;; => Projects where Charlie is a member

(charlie/step-2-tasks)
;; => Tasks assigned to Charlie directly OR via Engineering group

(charlie/step-3-allocations)
;; => Allocations WITH full task details visible
;;    (contrast with Diana who sees user_task: nil)

(charlie/step-4-group-access)
;; => Alpha Task 3 visible via Engineering group
;;    RLS follows: Task.assignee_group → UserGroup.users → User
```

**Key insight:** Charlie sees task details in his allocations because he passes the Task guards. Diana doesn't.

---

### Act 3: Alice's Domain (The Project Owner)

Alice owns Project Alpha. She has full control within her domain.

**Run as Alice:**

```clojure
(alice/step-1-projects)
;; => Only Project Alpha (she owns it)

(alice/step-2-tasks)
;; => ALL Alpha tasks, regardless of who's assigned

(alice/step-3-cross-project-isolation)
;; => Cannot see Project Beta or its tasks

(alice/step-4-grant-access)
;; => Successfully adds Charlie as project member
;;    (owners can modify their own projects)
```

**Key insight:** Ownership gives full access within domain, but zero access outside it.

---

### Act 4: Bob's Isolation (The Other Owner)

Bob owns Project Beta. He demonstrates complete isolation between peer owners.

**Run as Bob:**

```clojure
(bob/step-1-projects)
;; => Only Project Beta

(bob/step-2-tasks)
;; => Only Beta tasks

(bob/step-3-isolation)
;; => THROWS: "Write access denied by RLS"
;;    Bob cannot modify Alice's project
```

**Key insight:** Two owners, two domains, zero cross-contamination.

---

## Data Model

```
┌─────────────────────────────────────────────────────────────────────┐
│                      PROJECT MANAGEMENT DATASET                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   ┌──────────┐         ┌─────────────────┐         ┌──────────┐     │
│   │   User   │◄────────│     Project     │────────►│   User   │     │
│   │  (owner) │ owner   │                 │ members │ (members)│     │
│   └──────────┘         └────────┬────────┘         └──────────┘     │
│                                 │                                    │
│                                 │ tasks                              │
│                                 ▼                                    │
│                        ┌─────────────────┐                          │
│                        │   ProjectTask   │                          │
│                        │                 │                          │
│                        │  • assignee ────┼──────────► User          │
│                        │  • assignee_group ─────────► UserGroup     │
│                        └────────┬────────┘              │           │
│                                 │                       │ users     │
│                                 │                       ▼           │
│                                 │                    ┌──────────┐   │
│                                 │                    │   User   │   │
│                                 │                    └──────────┘   │
└─────────────────────────────────┼───────────────────────────────────┘
                                  │
                                  │ user_task (cross-dataset reference)
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     RESOURCE PLANNING DATASET                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│                        ┌─────────────────┐                          │
│                        │   Allocation    │                          │
│                        │                 │                          │
│                        │  • hours        │                          │
│                        │  • notes        │                          │
│                        │  • user ────────┼──────────► User          │
│                        │  • user_task ───┼──────────► ProjectTask   │
│                        └─────────────────┘              (guarded!)  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## RLS Guard Types Demonstrated

| Type | Example | Path |
|------|---------|------|
| **Direct Reference** | Project owner | `Project.project_owner → User` |
| **N:N Relation** | Project members | `Project.members → User` |
| **Multihop** | Group assignment | `Task.assignee_group → UserGroup.users → User` |
| **Cross-Dataset** | Allocation → Task | Guards apply even when accessed from another dataset |

---

## Guard Configuration

```
Project:
  ├── Guard 1: members → User           [READ]
  └── Guard 2: project_owner → User     [READ, WRITE, DELETE]

ProjectTask:
  ├── Guard 1: assignee → User          [READ]
  ├── Guard 2: assignee_group.users     [READ] (multihop)
  └── Guard 3: project.project_owner    [READ, WRITE, DELETE]
```

---

## Key Takeaways

1. **RLS is declarative** - Define guard paths once, enforcement is automatic
2. **Cross-dataset boundaries respected** - Guards apply even when data is accessed via foreign keys from another dataset
3. **Dynamic access** - Add user to group/members = instant access change (no code deploy)
4. **Aggregates respect RLS** - `_agg`, `_count` only compute over authorized rows
5. **Write guards** - Same mechanism protects mutations, not just reads

---

## Troubleshooting

**"Write access denied by RLS"**
- Check if user is project owner (only owners can write)
- Verify you're logged in as the correct user (`eywa.edn` in REPL folder)

**Empty results when expecting data**
- Run `(eywa.client/open-pipe)` to establish session
- Check if data was loaded by running as admin first

**user_task is null (expected for Diana, not for Charlie)**
- Diana: Correct - she doesn't pass Task guards
- Charlie: Check if he's assigned to the task or in the assignee_group

---

## Files

```
demo/
├── src/demo/
│   ├── core.clj        # GraphQL wrappers, dataset import
│   ├── data.clj        # Stable EUUIDs, demo entities
│   ├── alice.clj       # Owner demo
│   ├── bob.clj         # Isolation demo
│   ├── charlie.clj     # Team member demo
│   └── diana.clj       # Cross-dataset RLS demo (KEY!)
├── repl/
│   ├── admin/          # Superuser session
│   ├── alice/          # Alice's session
│   ├── bob/            # Bob's session
│   ├── charlie/        # Charlie's session
│   └── diana/          # Diana's session
└── resources/
    └── *.json          # Dataset definitions
```
