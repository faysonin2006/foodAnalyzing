package recipes.recipesfromdbservice.configs.exceptionhandler.exceptions;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message){
        super(message);
    }
}
