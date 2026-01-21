package com.mayday.ai.controller;

import com.mayday.ai.api.AiService;
import com.mayday.ai.controller.req.CreateAiKeyReq;
import com.mayday.ai.model.vo.ChatReq;
import com.mayday.ai.model.vo.ChatRsp;
import com.mayday.ai.service.AiKeyService;
import com.mayday.common.web.AjaxResult;
import org.checkerframework.common.reflection.qual.Invoke;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * AI 智能助手控制器
 *
 * @author Antigravity
 * @since 1.0.0
 */
@RestController
@RequestMapping("/ai")
public class AiController
{

    @Autowired
    private AiKeyService aiKeyService;

    @Autowired
    private AiService aiService;

    @PostMapping("/chat")
    public AjaxResult chat(@RequestBody ChatReq req) {
        String result = aiService.chat(req.getSceneCode(), req.getTenantId(), req.getPrompt());
        return AjaxResult.success(new ChatRsp(result));
    }
    /**
     * 测试插入api-key
     * @param req
     * @return
     */
    @PostMapping("/createKey")
    public AjaxResult createKey(@RequestBody CreateAiKeyReq req)
    {
        Long keyId = aiKeyService.createKey(
            req.getProvider(),
            req.getName(),
            req.getApiKey(),
            req.getRemark()
        );

        return AjaxResult.success(keyId);
    }

}
