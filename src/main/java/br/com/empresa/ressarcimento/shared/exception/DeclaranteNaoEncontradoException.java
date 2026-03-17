package br.com.empresa.ressarcimento.shared.exception;

public class DeclaranteNaoEncontradoException extends RuntimeException {

    public DeclaranteNaoEncontradoException() {
        super("Declarante não cadastrado. Cadastre os dados do declarante antes de gerar arquivos.");
    }

    public DeclaranteNaoEncontradoException(String message) {
        super(message);
    }
}
