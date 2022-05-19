package org.bots.tinkoff.service;
import org.bots.tinkoff.model.Bot;

public class TradeService implements Runnable{
    private Bot bot;

    public TradeService(Bot bot) {
        this.bot = bot;
    }

    @Override
    public void run() {
        bot.sendMessage("Im bot, Trading ...\n");
        bot.getCandlesDay();
    }

}
