package org.knowm.xchange.huobi.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.dto.Order.IOrderFlags;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.huobi.HuobiUtils;
import org.knowm.xchange.huobi.dto.trade.HuobiCreateOrderRequest;
import org.knowm.xchange.huobi.dto.trade.HuobiOrder;
import org.knowm.xchange.huobi.dto.trade.TimeInForce;
import org.knowm.xchange.huobi.dto.trade.results.HuobiCancelOrderResult;
import org.knowm.xchange.huobi.dto.trade.results.HuobiOrderInfoResult;
import org.knowm.xchange.huobi.dto.trade.results.HuobiOrderResult;
import org.knowm.xchange.huobi.dto.trade.results.HuobiOrdersResult;
import org.knowm.xchange.service.trade.params.TradeHistoryParams;
import org.knowm.xchange.service.trade.params.TradeHistoryParamsAll;
import org.knowm.xchange.utils.Assert;

class HuobiTradeServiceRaw extends HuobiBaseService {

  HuobiTradeServiceRaw(Exchange exchange) {
    super(exchange);
  }

  HuobiOrder[] getHuobiTradeHistory(TradeHistoryParams tradeHistoryParams) throws IOException {
    Assert.isTrue(
        tradeHistoryParams instanceof TradeHistoryParamsAll,
        "You need to provide the currency pair to get the user trades.");

    TradeHistoryParamsAll params = (TradeHistoryParamsAll) tradeHistoryParams;
    String tradeStates = "partial-filled,partial-canceled,filled";
    String symbol = params.getCurrencyPair().toString().replaceAll("/", "").toLowerCase();
    ;
    HuobiOrdersResult result =
        huobi.getOpenOrders(
            symbol,
            tradeStates,
            params.getStartTime().getTime(),
            params.getEndTime().getTime(),
            exchange.getExchangeSpecification().getApiKey(),
            HuobiDigest.HMAC_SHA_256,
            2,
            HuobiUtils.createUTCDate(exchange.getNonceFactory()),
            signatureCreator);
    return checkResult(result);
  }

  HuobiOrder[] getHuobiOpenOrders() throws IOException {
    String states = "pre-submitted,submitted,partial-filled";
    HuobiOrdersResult result =
        huobi.getOpenOrders(
            null,
            states,
            null,
            null,
            exchange.getExchangeSpecification().getApiKey(),
            HuobiDigest.HMAC_SHA_256,
            2,
            HuobiUtils.createUTCDate(exchange.getNonceFactory()),
            signatureCreator);
    return checkResult(result);
  }

  String cancelHuobiOrder(String orderId) throws IOException {
    HuobiCancelOrderResult result =
        huobi.cancelOrder(
            orderId,
            exchange.getExchangeSpecification().getApiKey(),
            HuobiDigest.HMAC_SHA_256,
            2,
            HuobiUtils.createUTCDate(exchange.getNonceFactory()),
            signatureCreator);
    return checkResult(result);
  }

  String placeHuobiLimitOrder(LimitOrder limitOrder) throws IOException {
    String type;
    if (limitOrder.getType() == OrderType.BID) {
      type = "buy-limit";
    } else if (limitOrder.getType() == OrderType.ASK) {
      type = "sell-limit";
    } else {
      throw new ExchangeException("Unsupported order type.");
    }

    ////////////////////////////////////////////////////////////////////////////////
    {
      Set<IOrderFlags> orderFlags = limitOrder.getOrderFlags();
      Iterator<IOrderFlags> orderFlagsIterator = orderFlags.iterator();

      while (orderFlagsIterator.hasNext()) {
        IOrderFlags orderFlag = orderFlagsIterator.next();
        if (orderFlag instanceof TimeInForce) {
          if (limitOrder.getType() == OrderType.BID) {
            type = "buy-ioc";
          } else if (limitOrder.getType() == OrderType.ASK) {
            type = "sell-ioc";
          }
        }
      }
    }
    ////////////////////////////////////////////////////////////////////////////////
    HuobiOrderResult result =
        huobi.placeLimitOrder(
            new HuobiCreateOrderRequest(
                getAccountId(),
                limitOrder.getOriginalAmount().toString(),
                limitOrder.getLimitPrice().toString(),
                HuobiUtils.createHuobiCurrencyPair(limitOrder.getCurrencyPair()),
                type),
            exchange.getExchangeSpecification().getApiKey(),
            HuobiDigest.HMAC_SHA_256,
            2,
            HuobiUtils.createUTCDate(exchange.getNonceFactory()),
            signatureCreator);

    return checkResult(result);
  }

  String placeHuobiMarketOrder(MarketOrder limitOrder) throws IOException {
    String type;
    if (limitOrder.getType() == OrderType.BID) {
      type = "buy-market";
    } else if (limitOrder.getType() == OrderType.ASK) {
      type = "sell-market";
    } else {
      throw new ExchangeException("Unsupported order type.");
    }
    HuobiOrderResult result =
        huobi.placeMarketOrder(
            new HuobiCreateOrderRequest(
                getAccountId(),
                limitOrder.getOriginalAmount().toString(),
                null,
                HuobiUtils.createHuobiCurrencyPair(limitOrder.getCurrencyPair()),
                type),
            exchange.getExchangeSpecification().getApiKey(),
            HuobiDigest.HMAC_SHA_256,
            2,
            HuobiUtils.createUTCDate(exchange.getNonceFactory()),
            signatureCreator);
    return checkResult(result);
  }

  List<HuobiOrder> getHuobiOrder(String... orderIds) throws IOException {
    List<HuobiOrder> orders = new ArrayList<>();
    for (String orderId : orderIds) {
      HuobiOrderInfoResult orderInfoResult =
          huobi.getOrder(
              orderId,
              exchange.getExchangeSpecification().getApiKey(),
              HuobiDigest.HMAC_SHA_256,
              2,
              HuobiUtils.createUTCDate(exchange.getNonceFactory()),
              signatureCreator);
      orders.add(checkResult(orderInfoResult));
    }
    return orders;
  }

  private String getAccountId() throws IOException {
    return String.valueOf(
        ((HuobiAccountServiceRaw) exchange.getAccountService()).getAccounts()[0].getId());
  }
}
