package com.qblog.knowledge;

import com.qblog.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/knowledge/tokens")
public class KnowledgeTokenController {
    private final KnowledgeTokenService tokens;
    @GetMapping public Result<List<KnowledgeTokenService.TokenView>> list() { return Result.success(tokens.list()); }
    @PostMapping public Result<KnowledgeTokenService.TokenView> create(@RequestBody KnowledgeTokenService.CreateRequest request) {
        return Result.success(tokens.create(request));
    }
    @DeleteMapping("/{id}") public Result<Void> revoke(@PathVariable Long id) { tokens.revoke(id); return Result.success(); }
}
