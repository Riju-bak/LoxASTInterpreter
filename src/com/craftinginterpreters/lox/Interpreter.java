package com.craftinginterpreters.lox;

public class Interpreter implements Expr.Visitor<Object> {
    @Override
    public Object visitLiteralExpr(Expr.Literal expr){
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr){
        return evaluate(expr.expression);
    }

    private Object evaluate(Expr expr){
        //TODO(Rijubak): Won't this recurse infinitely? Ans: NO, every GroupingExpr has a field named `Expr expression`, which could be \
//        TODO(Cont.) Binary, Unary or Literal.

        // evaluate() does a post order traversal of the AST:)
        return expr.accept(this);
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

    private void checkNumberOperand(Token operator, Object operand){
        if(operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }


    private void checkNumberOperand(Token operator, Object left, Object right){
        if(left instanceof  Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isTruthy(Object object){
        // Lox follows Ruby’s simple rule: false and nil are falsey, and everything else is truthy
        if(object == null) return false;
        if(object instanceof Boolean) return (Boolean) object;
        return true;
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

    private boolean isEqual(Object a, Object b){
        if(a==null && b==null) return true;
        if(a == null) return false;

        return a.equals(b);
   }

    void interpret(Expr expression){
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        }
        catch (RuntimeError error){
            Lox.runtimeError(error);
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


}

























