package br.com.empresa.ressarcimento.ui;

import br.com.empresa.ressarcimento.shared.exception.DeclaranteNaoEncontradoException;
import br.com.empresa.ressarcimento.shared.exception.RecursoNaoEncontradoException;
import jakarta.validation.ConstraintViolationException;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Order(0)
@ControllerAdvice(basePackages = "br.com.empresa.ressarcimento.ui")
public class UiExceptionHandler {

    private static final String ERROR_VIEW = "ui/error";

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView constraintViolation(ConstraintViolationException ex) {
        return new ModelAndView(ERROR_VIEW, "mensagem", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView validacao(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return new ModelAndView(ERROR_VIEW, "mensagem", msg.isEmpty() ? "Dados inválidos." : msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView illegalArgument(IllegalArgumentException ex) {
        log.debug("UI bad request: {}", ex.getMessage());
        return new ModelAndView(ERROR_VIEW, "mensagem", ex.getMessage());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView recursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        return new ModelAndView(ERROR_VIEW, "mensagem", ex.getMessage());
    }

    @ExceptionHandler(DeclaranteNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView declaranteNaoEncontrado(DeclaranteNaoEncontradoException ex) {
        return new ModelAndView(
                ERROR_VIEW,
                "mensagem",
                "Nenhum declarante cadastrado. Cadastre o declarante antes de usar esta função.");
    }

    @ExceptionHandler(JAXBException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView jaxb(JAXBException ex) {
        log.warn("Erro JAXB na UI", ex);
        return new ModelAndView(ERROR_VIEW, "mensagem", "Erro ao gerar XML: " + ex.getMessage());
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView io(IOException ex) {
        log.warn("Erro de E/S na UI", ex);
        return new ModelAndView(ERROR_VIEW, "mensagem", "Erro ao processar arquivo: " + ex.getMessage());
    }
}
