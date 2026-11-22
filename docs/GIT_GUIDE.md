# Git and GitHub guide

You run these commands yourself - the commit history must reflect your own work, so nothing here is pre-made.
Run them in the project folder (the one containing `pom.xml`). Commit after each step *when it works*, using your own words in the messages if you like.

## One-time setup

```bash
git config --global user.name "Umama Wahab"
git config --global user.email "your-email@example.com"
```

## Start the repository

```bash
git init                      # create the local repository
git status                    # see what is untracked / changed
git add pom.xml .gitignore README.md src/main/java/com/stockly/Main.java    # stage specific files
git commit -m "Initial JavaFX project setup"
```

## Suggested commits

Because the project is already complete, create the history by staging the matching files for each step. Use `git status` before each commit to check what is staged, and only commit things that belong to that step.

| # | Commit message | Files to stage (git add ...) |
|---|---|---|
| 1 | `Initial JavaFX project setup` | `pom.xml .gitignore README.md src/main/java/com/stockly/Main.java src/main/java/com/stockly/AppInfo.java` |
| 2 | `Create Product model and inventory manager` | `src/main/java/com/stockly/model src/main/java/com/stockly/service` |
| 3 | `Implement inventory CRUD operations` | `src/main/java/com/stockly/controller/InventoryController.java src/main/java/com/stockly/controller/ProductDialogController.java src/main/resources/com/stockly/view/InventoryView.fxml src/main/resources/com/stockly/view/ProductDialog.fxml` |
| 4 | `Design dashboard interface` | `src/main/resources/com/stockly/css src/main/resources/com/stockly/view/MainView.fxml src/main/resources/com/stockly/view/DashboardView.fxml src/main/java/com/stockly/ui src/main/java/com/stockly/controller/MainController.java src/main/java/com/stockly/controller/DashboardController.java src/main/java/com/stockly/controller/AppShell.java src/main/java/com/stockly/controller/Page.java src/main/java/com/stockly/InventoryApplication.java` |
| 5 | `Add reports page` | `src/main/java/com/stockly/controller/ReportsController.java src/main/resources/com/stockly/view/ReportsView.fxml` |
| 6 | `Add data persistence` | `src/main/java/com/stockly/persistence` |
| 7 | `Add unit tests and test plan` | `src/test docs/TEST_CASES.md` |
| 8 | `Add Javadoc documentation` | `docs/javadoc docs/ARCHITECTURE.md` |
| 9 | `Final UI improvements and testing` | `docs README.md` and anything else still showing in `git status` |

Example for commit 2:

```bash
git add src/main/java/com/stockly/model src/main/java/com/stockly/service
git status
git diff --staged             # review exactly what will be committed
git commit -m "Create Product model and inventory manager"
```

> Note: the controllers and views are linked, so the project only compiles once all of them are present.
> That is fine for a history - but if you want every commit to compile, commit in this order and add the files of a later step only when it is complete,
> or simply combine steps 3-5 into one commit.

## Everyday commands

```bash
git status                    # what changed?
git diff                      # unstaged changes line by line
git diff --staged             # staged changes line by line
git log --oneline             # short history
git log --oneline --graph --decorate
git log -p -1                 # last commit with its changes
```

## Publish on GitHub

1. On github.com click **New repository**, name it `stockly`, leave it empty (no README, no .gitignore), create it.
2. Then:

```bash
git branch -M main
git remote add origin https://github.com/<your-username>/stockly.git
git remote -v                 # check the address
git push -u origin main       # first push
git push                      # later pushes
```

If GitHub asks for a password, use a **Personal Access Token** (Settings -> Developer settings -> Personal access tokens) or sign in with GitHub CLI (`gh auth login`).

## Good habits for the assessment

- Small commits with meaningful messages (what and why), not "update" or "final".
- Never commit `target/` (already in `.gitignore`).
- Commit `docs/javadoc/` - the assignment requires the generated documentation in the submission.
- Show `git log --oneline` during the viva and be ready to explain any commit.
