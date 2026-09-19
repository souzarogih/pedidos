package br.com.pedidos.api.adapters.entrada.rest;

import br.com.pedidos.api.application.pedido.PedidoNaoEncontradoException;
import br.com.pedidos.api.application.pedido.PedidoSemItensException;
import br.com.pedidos.api.domain.pedido.ItemInvalidoException;
import br.com.pedidos.api.domain.pedido.PedidoFechadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PedidoExceptionHandler {

    @ExceptionHandler(ItemInvalidoException.class)
    public ResponseEntity<ErroResponse> tratarItemInvalido(ItemInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErroResponse(ex.getMessage()));
    }

    @ExceptionHandler(PedidoSemItensException.class)
    public ResponseEntity<ErroResponse> tratarPedidoSemItens(PedidoSemItensException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErroResponse(ex.getMessage()));
    }

    @ExceptionHandler(PedidoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarPedidoNaoEncontrado(PedidoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErroResponse(ex.getMessage()));
    }

    @ExceptionHandler(PedidoFechadoException.class)
    public ResponseEntity<ErroResponse> tratarPedidoFechado(PedidoFechadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErroResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(new ErroResponse("dados inválidos"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarJsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErroResponse("JSON malformado"));
    }
}
