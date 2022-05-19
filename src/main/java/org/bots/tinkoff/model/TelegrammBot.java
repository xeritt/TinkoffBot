package org.bots.tinkoff.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelegrammBot extends TelegramLongPollingBot {

    public static String COMMAND_PREFIX = "/";

    @Value("${bot.username}")
    private String username;

    @Value("${bot.token}")
    private String token;

    @Value("${tinkoffbot.token}")
    private String tinkofftoken;

    @Value("${tinkoffbot.figa}")
    private String figa;

    public String chatId;

    public String getChatId() {
        return chatId;
    }

    @Override
    public void onUpdateReceived(Update update) {

        if(update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText().trim();
            SendMessage sm = new SendMessage();

            Bot bot = new Bot();
            bot.init(tinkofftoken, figa, this);

            chatId = update.getMessage().getChatId().toString();
            BotAnswer answer = bot.execCommands(message);

            sm.setChatId(chatId);
            sm.setText(answer.getText());
            addKeyBoard(answer, sm);

            try {
                execute(sm);
            } catch (TelegramApiException e) {
                //todo add logging to the project.
                e.printStackTrace();
            }
        }
    }

    private void addKeyBoard(BotAnswer answer, SendMessage sm) {
        ReplyKeyboardMarkup keys = new ReplyKeyboardMarkup();
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        if (answer.getBtms()!=null) {
            for (String btm : answer.getBtms()) {
                row.add(new KeyboardButton(btm));
            }
            rows.add(row);
            keys.setKeyboard(rows);
            sm.setReplyMarkup(keys);
        }
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }
}
