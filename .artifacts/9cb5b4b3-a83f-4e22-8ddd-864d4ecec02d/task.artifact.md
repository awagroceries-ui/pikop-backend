# 📌 Task Checklist: Update Knowledge Base Articles & EJS Views for Active Platform Fees

- `[/]` Task 1: Create Database Migration & Maintenance Script (`backend_v3`)
  - `[ ]` Write `1726960000000_update_kb_fee_articles.js` migration updating `knowledge_base` articles (5% COD fee, 20% dispatch commission / 80% fulfiller share, 5% merchant commission)
  - `[ ]` Write `update_kb_fees.js` standalone execution script

- `[ ]` Task 2: Update Admin EJS Views (`backend_v3/src/views/`)
  - `[ ]` Update `guest_checkout.ejs` (`Platform Fee (5%)`)
  - `[ ]` Update `financial_overview.ejs` (`Commission (20%)` and `Escrow Fees (5%)`)
  - `[ ]` Update `fleet_partners.ejs` (`DEFAULT (20%)`)

- `[ ]` Task 3: Git Automation & VPS Execution
  - `[ ]` Stage, commit, and push changes to GitHub `main`
  - `[ ]` Provide VPS deployment command prompts
