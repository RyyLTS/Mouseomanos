package com.riobranco.mouseomanos.produto_api.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // 400 - corpo com campos inválidos (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarValidacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> detalhes = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> detalhes.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos", req, detalhes);
    }

    // 400 - violações de constraints fora do @RequestBody
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResposta> tratarConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        Map<String, String> detalhes = new LinkedHashMap<>();
        ex.getConstraintViolations()
                .forEach(v -> detalhes.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos", req, detalhes);
    }

    // 400 - JSON malformado ou com tipo errado (ex.: "valor": "abc")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> tratarJsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição ausente ou JSON malformado", req, null);
    }

    // 400 - parâmetro/ID com tipo inválido (ex.: /produtos/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> tratarTipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String mensagem = "O parâmetro '" + ex.getName() + "' possui um valor inválido: '" + ex.getValue() + "'";
        return resposta(HttpStatus.BAD_REQUEST, mensagem, req, null);
    }

    // 400 - parâmetro obrigatório ausente
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResposta> tratarParametroAusente(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "O parâmetro '" + ex.getParameterName() + "' é obrigatório", req, null);
    }

    // 404 - produto inexistente
    @ExceptionHandler(ProdutoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> tratarNaoEncontrado(ProdutoNaoEncontradoException ex, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    // 404 - rota inexistente
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResposta> tratarRotaInexistente(NoResourceFoundException ex, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, "Recurso não encontrado: " + req.getRequestURI(), req, null);
    }

    // 405 - método HTTP não permitido
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResposta> tratarMetodo(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método " + ex.getMethod() + " não permitido para este recurso", req, null);
    }

    // 406 - formato de resposta não aceito
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErroResposta> tratarNaoAceito(HttpMediaTypeNotAcceptableException ex, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_ACCEPTABLE, "Formato de resposta não suportado", req, null);
    }

    // 415 - Content-Type não suportado
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResposta> tratarMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return resposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type não suportado. Use application/json", req, null);
    }

    // 409 - violação de integridade no banco
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> tratarIntegridade(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violação de integridade em {}: {}", req.getRequestURI(), ex.getMessage());
        return resposta(HttpStatus.CONFLICT, "Operação viola uma restrição de integridade dos dados", req, null);
    }

    // Status explícitos lançados via ResponseStatusException
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResposta> tratarStatus(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String mensagem = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return resposta(status, mensagem, req, null);
    }

    // 500 - qualquer erro inesperado (detalhes só no log)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> tratarGenerico(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {} {}", req.getMethod(), req.getRequestURI(), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", req, null);
    }

    private ResponseEntity<ErroResposta> resposta(HttpStatus status, String mensagem,
                                                  HttpServletRequest req, Map<String, String> detalhes) {
        ErroResposta corpo = new ErroResposta(Instant.now(), status.value(), status.getReasonPhrase(),
                mensagem, req.getRequestURI(), detalhes);
        return ResponseEntity.status(status).body(corpo);
    }
}
