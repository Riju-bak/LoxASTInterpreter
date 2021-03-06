package com.craftinginterpreters.lox;

import java.util.List;
import java.util.ArrayList;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{

    // environment field changes as we enter and exit scopes. It tracks the current environment.
    // globals holds a fixed reference to the outermost global environment.
    final Environment globals = new Environment();
    private Environment environment = globals;

    Interpreter(){
        // we provide user with a clock() function that they can use, when writing lox code.
        globals.define("clock", new LoxCallable() {
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis()/1000.0;
            }

            @Override
            public int arity() {
                return 0;
            }

            @Override
            public String toString(){
                // TODO: Figure out the use of this function?
                return "<native fn>";
            }
        });
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr){
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr){
        return evaluate(expr.expression);
    }


    @Override
    public Object visitUnaryExpr(Expr.Unary expr){
        Object right = evaluate(expr.right);

        switch(expr.operator.type){
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right; //TODO(Rijubak): What if cast fails?

            case BANG:
                return !isTruthy(right);
        }
        //Unreachable
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr){
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch(expr.operator.type){
            case GREATER:
                checkNumberOperand(expr.operator, left, right);
                return (double)left > (double)right;

            case LESS:
                checkNumberOperand(expr.operator, left, right);
                return (double)left < (double)right;

            case GREATER_EQUAL:
                checkNumberOperand(expr.operator, left, right);
                return (double)left >= (double)right;

            case LESS_EQUAL:
                checkNumberOperand(expr.operator, left, right);
                return (double)left <= (double)right;

            case BANG_EQUAL:
                checkNumberOperand(expr.operator, left, right);
                return !isEqual(left, right);

            case EQUAL_EQUAL:
                checkNumberOperand(expr.operator, left, right);
                return isEqual(left, right);

            case MINUS:
                checkNumberOperand(expr.operator, left, right);
                return (double)left - (double)right;

            case SLASH:
                checkNumberOperand(expr.operator, left, right);
                return (double)left / (double)right;

            case STAR:
                checkNumberOperand(expr.operator, left, right);
                return (double)left * (double)right;

            case PLUS:
                if(left instanceof Double && right instanceof Double){
                    return (double)left +(double)right;
                }
                if(left instanceof String && right instanceof String){
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
        }

        //Unreachable
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr){
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }


    @Override
    public Object visitLogicalExpr(Expr.Logical expr){
        Object left = evaluate(expr.left);

        if(expr.operator.type == TokenType.OR){
            if(isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }
        return evaluate(expr.right);
    }

    @Override
    public Object visitCallExpr(Expr.Call expr){
//        First, we evaluate the expression for the callee.
//        Typically, this expression is just an identifier that looks up the function by its name, but it could be anything.
//        Then we evaluate each of the argument expressions in order and store the resulting values in a list.
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for(Expr argument : expr.arguments){
            arguments.add(evaluate(argument));
        }

        if(!(callee instanceof LoxCallable)){
            // this takes care of bad-calls like "totally not a function"();
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        LoxCallable function = (LoxCallable)callee;
        if(arguments.size() != function.arity()){
            throw new RuntimeError(expr.paren, "Expected "+
                    function.arity() + "arguments, but got " +
                    arguments.size() + ".");
        }
        return function.call(this, arguments);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt){
        evaluate(stmt.expression); //TODO: What is the point of this line, where is the evaluated expression being used?
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt){
        while(isTruthy(evaluate(stmt.condition))){
            execute(stmt.body);
        }
        return null;
    }


    @Override
    public Void visitIfStmt(Stmt.If stmt){
        if(isTruthy(evaluate(stmt.condition))){
            execute(stmt.thenBranch);
        } else if(stmt.elseBranch != null){
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt){
        Object value = evaluate(stmt.expression);
        System.out.println(value);
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if(stmt.initializer != null){
            value = evaluate(stmt.initializer);
        }
            environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt){
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt){
        //TODO: Incomplete
        return null;
    }
    private Object evaluate(Expr expr){
        //TODO(Rijubak): Won't this recurse infinitely? Ans: NO, every GroupingExpr has a field named `Expr expression`, which could be
//        TODO(Cont.) Binary, Unary or Literal.

        // evaluate() does a post order traversal of the AST:)
        return expr.accept(this); //if expr is Binary Expression then eventually Interpreter::visitBinaryExpr() will be called.
    }




//    void interpret(Expr expression){
//        try {
//            Object value = evaluate(expression);
//            System.out.println(stringify(value));
//        }
//        catch (RuntimeError error){
//            Lox.runtimeError(error);
//        }
//    }

    void interpret(List<Stmt> statements){
        try {
            for (Stmt stmt : statements){
                execute(stmt);
            }
        }
        catch (RuntimeError error){
            Lox.runtimeError(error);
        }
    }

    private void execute(Stmt stmt){
        //NOTE: comments show C++ style calls.
        stmt.accept(this); //for a print statement essentially Interpreter::visitPrintStmt() will be called.
        //for any expression statement Interpreter::visitExpressionStmt() will be called;
    }

    void executeBlock(List<Stmt> statements, Environment environment){
        Environment previous = this.environment;
        try{
            this.environment = environment;
            for(Stmt statement : statements){
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    private String stringify(Object object){
        if(object == null) return "nil";
        if(object instanceof Double){
            String text = object.toString();
            if(text.endsWith(".0")){
                text = text.substring(0, text.length()-2);
                return text;
            }
        }
        return object.toString();
    }

    private void checkNumberOperand(Token operator, Object operand){
        if(operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }


    private void checkNumberOperand(Token operator, Object left, Object right){
        if(left instanceof  Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isTruthy(Object object){
        // Lox follows Ruby???s simple rule: false and nil are falsey, and everything else is truthy
        if(object == null) return false;
        if(object instanceof Boolean) return (Boolean) object;
        return true;
    }



    private boolean isEqual(Object a, Object b){
        if(a==null && b==null) return true;
        if(a == null) return false;

        return a.equals(b);
    }
}


























