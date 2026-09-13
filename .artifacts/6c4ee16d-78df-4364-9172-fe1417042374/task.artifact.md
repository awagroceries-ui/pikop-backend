# Task Checklist: Fulfiller Category Split

- `[/]` **Part 1: Category Selection Step**
  - `[ ]` Create `FulfillerCategorySelectionScreen.kt` with 3 options (Foot Agent, Rider, Driver).
- `[ ]` **Part 2: Dynamic Fulfiller Signup Form**
  - `[ ]` Modify `SignupFulfillerScreen.kt` to take `category` argument.
  - `[ ]` Add common fields (DOB, Home Address, Gender).
  - `[ ]` Add conditional Vehicle fields (Make, Model, Color, Reg Num) for Rider/Driver.
  - `[ ]` Add conditional License/Insurance/Permit fields for Rider/Driver (stub file upload state).
- `[ ]` **Part 3: Navigation Routing**
  - `[ ]` Update `UserTypeSelectionScreen.kt` to route to `fulfiller_category_selection`.
  - `[ ]` Update `MainActivity.kt` with new routes.
- `[ ]` **Part 4: Verification & Git**
  - `[ ]` Build project.
  - `[ ]` Git commit and push.