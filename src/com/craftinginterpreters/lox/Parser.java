package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static com.craftinginterpreters.lox.TokenType.*;

/*
A parser really has two jobs:

1) Given a valid sequence of tokens, produce a corresponding syntax tree.

2) Given an invalid sequence of tokens, detect any errors and tell the user about their mistakes.
*/

//Below is a syntax tree for -123*(45.67)
//            *
//           /  \
//          -   ()
//          /    \
//         123   45.67

//The parser below is a recursive descent parser (RDP), meaning top to bottom in below table
// i.e. lowest precedence(expression) to highest(primary)


public class Parser {
// Lowest Precedence: //    expression
                      //    equality
                      //    comparison
                      //    term
                      //    factor
                      //    unary
// Highest Precedence //    primary

/******* Complete expression grammar ********************************/
//     expression     → assignment ;
//    assignment     → IDENTIFIER "=" assignment
//               | logic_or ;
//    logic_or       → logic_and ( "or" logic_and )* ;
//    logic_and      → equality ( "and" equality )* ;
//    equality       → comparison ( ( "!=" | "==" ) comparison )* ;
//    comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
//    term           → factor ( ( "-" | "+" ) factor )* ;
//    factor         → unary ( ( "/" | "*" ) unary )* ;
//    unary          → ( "!" | "-" ) unary
//                     | call ;
//    call           -> primary("(" arguments? ")")*;
//    primary        → NUMBER | STRING | "true" | "false" | "nil"
//            | "(" expression ")" | IDENTIFIER ;
/*  ******************************************************************/


/************Utility Rule (Not complete) *******************/
//    arguments -> expression("," expression)*;

/**********************************************************/



/*    unary can match to unary and primary
    factor can match to factor and unary(covers primary as well)
            .
            .
            .
    expression can match to expression, equality, comparison, term all the way down to primary
            i.e effectively expression matches to equality and equality covers the rest*/

/********** Statement Grammar (Not complete) **********/

//    block          → "{" declaration* "}" ;

//    declaration  → funDecl
//               | varDecl
//               | statement ;

//funcDel -> "fun" function;
//function -> IDENTIFIER "(" parameters? ")" block;

//    parameters     → IDENTIFIER ( "," IDENTIFIER )* ;

//    statement      → exprStmt
//               | printStmt
//               | block ;
//

/*****************************************/


    private static class ParseError extends RuntimeException{}
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

//    Expr parse(){
//        try{
//            return expression();
//        }
//        catch(ParseError error){
//            return null;
//        }
//    }

    List<Stmt> parse(){
        List<Stmt> statements = new ArrayList<>();
        while(!isAtEnd()){
            statements.add(declaration());
        }
        return statements;
    }

    private Stmt declaration(){
        try{
            if(match(FUN)) {
                return function("function");
            }
            if(match(VAR)){
                return varDeclaration();
            }
            return statement();
        } catch(ParseError error){
            synchronize();
            return null;
        }
    } 

    private Stmt function(String kind){
//        kind can be "function" or "method"
        Token name = consume(IDENTIFIER, "Expect " + kind + "name.");

        consume(LEFT_PAREN, "Expect '(' after " + kind + "name." );
        List<Token> parameters = new ArrayList<>();
        if(!check(RIGHT_PAREN)){
            do{
                if(parameters.size() >= 255){
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while(match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");
        consume(LEFT_BRACE, "Expect '{' before "+ kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    private Stmt varDeclaration(){
        Token name = consume(IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        if(match(EQUAL)){
            // for case like var myVar = 2*3+4;
            initializer = expression(); //here the expression 2*3+4
        }
        consume(SEMICOLON, "Expected ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }


    private Stmt statement(){
        if(match(FOR)) return forStatement();
        if(match(IF)) return ifStatement();
        if(match(PRINT)) return printStatement();
        if(match(WHILE)) return whileStatement();
        if(match(LEFT_BRACE)) return new Stmt.Block(block());
        return expressionStatement();
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expected ; after expression.");
        return new Stmt.Expression(expr);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expected ; after value.");
        return new Stmt.Print(value);
    }

    private Stmt ifStatement(){
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if(match(ELSE)){
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt whileStatement(){
        consume(LEFT_PAREN, "Expected '(' after while.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    private Stmt forStatement(){
        // NOTE: We don't have a visitForStmt in interpreter.java , because the for statement will be parsed
        // into a while statement form in the AST.
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if(match(SEMICOLON)){
            initializer = null;
        } else if(match(VAR)){
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if(!check(SEMICOLON)){
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if(!check(RIGHT_PAREN)){
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

/***********************************************/
        Stmt body = statement();
        if(increment != null){
            body = new Stmt.Block(
                    Arrays.asList(body, new Stmt.Expression(increment))
            );
        }

        if(condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if(initializer != null){
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

/*********************************************/
        return body;
    }


    private List<Stmt> block(){
        List<Stmt> statements = new ArrayList<>();
        while(!check(RIGHT_BRACE) && !isAtEnd()){
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr expression(){
//        expression     → assignment ;
        return assignment();
    }

    private Expr assignment(){
// assignment     → IDENTIFIER "=" assignment
//                | logic_or ;
        Expr expr = or();

        if(match(EQUAL)){
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable){
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    private Expr or(){
        Expr expr = and();

        while(match(OR)){
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr and(){
        Expr expr = equality();

        while(match(AND)){
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr equality(){
        // equality -> comparison (("!=" | "==") comparison)*
        // eg. a==b!=c==d==f!=g ...
        Expr expr = comparison();

        while(match(BANG_EQUAL, EQUAL_EQUAL)){
            Token operator = previous(); //during match() you'll advance once so that's why we use previous()
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison(){
//        comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
        Expr expr = term();
        while(match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)){
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term(){
//        term  → factor ( ( "-" | "+" ) factor )* ;
        Expr expr = factor();
        while(match(MINUS, PLUS)){
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor(){
//        factor  → unary ( ( "/" | "*" ) unary )*
        Expr expr = unary();
        while(match(SLASH, STAR)){
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary(){
//        unary → ( "!" | "-" ) unary | primary ;
        if(match(BANG, MINUS)){
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    private Expr call(){
//        First, we parse a primary expression, the “left operand” to the call.
//        Then, each time we see a (, we call finishCall() to parse the call expression
//        using the previously parsed expression as the callee.
//        The returned expression becomes the new expr and we loop to see if the result is itself called.

        Expr expr = primary();
        while(true){
            if(match(LEFT_PAREN)){
                expr = finishCall(expr);
            }else{
                break;
            }
        }
        return expr;
    }


    private Expr finishCall(Expr callee){
//    This is more or less the arguments grammar rule translated to code, except that we also handle the zero-argument case.
//    We check for that case first by seeing if the next token is ). If it is, we don’t try to parse any arguments.
        List<Expr> arguments = new ArrayList<>();
        if(!check(RIGHT_PAREN)){
            do{
                if(arguments.size() > 255){
                    // This part is just for compatibility with the byte-code VM version.
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while(match(COMMA));
        }
        Token param = consume(RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee, param, arguments);
    }


    private Expr primary(){
//        primary        → NUMBER | STRING | "true" | "false" | "nil"
//            | "(" expression ")" | IDENTIFIER ;
        if(match(FALSE)) return new Expr.Literal(false);
        if(match(TRUE)) return new Expr.Literal(true);
        if(match(NIL)) return new Expr.Literal(null);

        if(match(NUMBER, STRING)){
            return new Expr.Literal(previous().literal);
        }

        if(match(LEFT_PAREN)){
            Expr expr = expression();
            consume(RIGHT_PAREN, "Except ')' after expression.");
            return new Expr.Grouping(expr);
        }

        if (match(IDENTIFIER)){
            return new Expr.Variable(previous()); //previous() because if match() is true, current advances by 1
        }
        throw error(peek(), "Expect expression.");
    }

    private Token consume(TokenType type, String message){
        //TODO(Question): Will consume block code execution if the type doesn't match? ... since advance() won't get called.
        if(check(type)) return advance();
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message){
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize(){
        // when you know, there's a mistake in the current line, you just keep advancing all the way to the ';'
        // , ignoring everything else in that line.
        advance();
        while(!isAtEnd()){
            if(previous().type == SEMICOLON) return;

            //TODO(Rijubak): Figure out what this switch statement is for? What does it do?
            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }

    private boolean match(TokenType... types){
        // check if the current token has any of the given types
        // if yes, consume the token and return true,
        // if not, leave the current token alone and return false
        for(TokenType type : types){
            if(check(type)){
                advance();
                return true;
            }
        }
        return false;
    }

    private Token advance(){
        // essentially return the current token and advance onto the next one
        if(!isAtEnd()) current++;
        return previous();
    }

    private boolean check(TokenType type){
        // check if current tokentype matches the type passed to check()
        if(isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean isAtEnd(){
        // Check if you are at the last element of List<Tokens>
        // the last token will always be EOF
        return peek().type == EOF;
    }
    private Token peek(){
        // get the current token
        return tokens.get(current);
    }

    private Token previous(){
        // get the token one position before the current
        return tokens.get(current-1);
    }

}
