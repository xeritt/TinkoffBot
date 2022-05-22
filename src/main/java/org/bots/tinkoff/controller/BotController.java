package org.bots.tinkoff.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

//@RequestMapping("/")
@RestController
@Tag(name = "Bot Service", description = "Сервис бота")
public class BotController {

    @Operation(summary = "Статус работы бота Tinkoff")
    @GetMapping(value = "/", produces = "application/json")
    private List<String> getMovies() {
        List<String> list = new ArrayList();
        list.add("Status ok");
        return list;
    }

}