package com.hometalk.onepass.common.exception;

import com.hometalk.onepass.community.exception.PostException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PostException.class)
    public String handlePostException(PostException e, RedirectAttributes redirectAttributes) {
        log.error("Post Related Error: {}", e.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        String boardCode = (e.getBoardCode() != null) ? e.getBoardCode() : "free";
        return "redirect:/community/" + boardCode;
    }
}
