# Refactoring Opportunities

| Local | Cheiro no código | Refabricação | Nº Aluno |
|---|---|---|---|
| AiShotStrategy::computeShots() | Overly Complex Method (CC = 19) | Extract Method | A definir |
| GameWindow::addComponentsMenu() | Overly Complex Method (CC = 14) | Extract Method | A definir |
| GameWindow (linhas 188-195, 252-263, 350-361) | Duplicated Code | Extract Method | A definir |
| GameWindow::updateBoard() | Overly Complex Method (CC = 12) | Extract Method | 129869 |
| Move::processEnemyFire() | Overly Complex Method (CC = 36) | Extract Method | 129869 |
| Move::processEnemyFire() | Message Chains (Law of Demeter: getCategory()) | Hide Delegate | A definir |
| GameController::receiveShots() | Overly Complex Method (CC = 12) | Decompose Conditional | 129869 |
| GameController::evaluateShots() | Message Chains (Law of Demeter) | Hide Delegate | 129869 |
| Tasks::menu() | Overly Complex Method (CC = 19) | Extract Method | A definir |
| Main::main() | Overly Complex Method (CC = 12) | Extract Method | A definir |
| Game (shoot(), stillFloating()) | Message Chains (Law of Demeter) | Move Instance Method | 129869 |
| Server (add(), post(), shotsResult()) | Message Chains (Law of Demeter) | Hide Delegate | 129869 |
| IMove / Move / Game | Inappropriate Intimacy (Cyclic Class Dependency) | Extract Interface | A definir |
| Fleet / Ship | Inappropriate Intimacy (Cyclic Class Dependency) | Move Instance Method | A definir |
| GameTest | Large Class (Overly Complex Class, CC = 108) | Extract Class | 129869 |