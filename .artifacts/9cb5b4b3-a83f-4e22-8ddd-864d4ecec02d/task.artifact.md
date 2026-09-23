# 📌 Task Checklist: Profile Photo Upload Multer Field Fix

- `[/]` Task 1: Backend Profile Photo Field Handling Fix
  - `[ ]` Update `fulfillerRoutes.js` to use `upload.single('photo')` / flexible upload middleware
  - `[ ]` Update `fulfillerController.js` (`uploadProfilePhoto`) to handle `req.file` or `req.files` flex field names

- `[ ]` Task 2: Android App Image Upload Field Alignment
  - `[ ]` Update `KycUploadScreen.kt` to send `"photo"` as field name in `MultipartBody.Part`

- `[ ]` Task 3: Git & VPS Deployment
  - `[ ]` Stage, commit, and push changes to GitHub `main`
  - `[ ]` Provide VPS deployment command prompts
