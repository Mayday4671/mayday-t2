package com.mayday.ai.controller;

import com.mayday.common.web.AjaxResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 智能助手控制器
 *
 * @author Antigravity
 * @since 1.0.0
 */
import org.springframework.web.bind.annotation.RequestParam;

/**
 * AI 智能助手控制器
 *
 * @author Antigravity
 * @since 1.0.0
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    @GetMapping("/chat")
    public AjaxResult chat(@RequestParam("query") String query) {
        return AjaxResult.success("Hello from AI! You asked: " + query);
    }
}
