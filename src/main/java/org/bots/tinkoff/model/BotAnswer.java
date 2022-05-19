package org.bots.tinkoff.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
public class BotAnswer {
    private List<String> btms = new ArrayList<>();
    private StringBuilder message = new StringBuilder();

    public BotAnswer() { }

    public BotAnswer(String text) {
        append(text);
    }

    public void addBtm(String text){
        btms.add(text);
    }

    public void append(String text){
        message.append(text);
    }

    public void appendln(String text){
        message.append(text + "\n");
    }

    public String getText(){
        return message.toString();
    }

}
