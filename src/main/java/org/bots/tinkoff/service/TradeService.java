package org.bots.tinkoff.service;
import org.bots.tinkoff.model.Bot;
import org.bots.tinkoff.model.BotAnswer;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.OrderState;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TradeService implements Runnable{

    private Bot bot;
    private final int limit = 1;
    static private int countOrder = 0;

    public TradeService(Bot bot) {
        this.bot = bot;
    }

    @Override
    public void run() {
        bot.sendMessage("Im bot, Trading ...\n");
        //bot.showCandlesDay();
        List<HistoricCandle> candles = bot.getHistoricCandles(CandleInterval.CANDLE_INTERVAL_DAY);
        List<OrderState> orders = bot.getOrderState(bot.getApi(), bot.getAccount());

        if (orders.size() < limit) {
          countOrder++;
          BigDecimal open = bot.getPrice(candles.get(0).getOpen());
          BigDecimal close = bot.getPrice(candles.get(candles.size() - 1).getClose());
          bot.sendMessage("O:"+ open + " C:"+ close);
          int cmp = open.compareTo(close);
          OrderDirection direction;
          if (cmp > 0) {
              bot.sendMessage("Strategy Sell");
              direction = OrderDirection.ORDER_DIRECTION_SELL;
          } else {
              bot.sendMessage("Strategy Buy");
              direction = OrderDirection.ORDER_DIRECTION_BUY;
          }
          BotAnswer answer = bot.ordersService(bot.getApi(), bot.getFigi(), bot.getAccount(), direction);
          bot.sendMessage(answer.getText());
        }

    }

}
