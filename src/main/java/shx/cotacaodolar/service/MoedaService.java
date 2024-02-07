package shx.cotacaodolar.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import shx.cotacaodolar.model.Moeda;
import shx.cotacaodolar.model.Periodo;

@Service
public class MoedaService {

        // o formato da data que o método recebe é "MM-dd-yyyy"
        public List<Moeda> getCotacoesPeriodo(String startDate, String endDate)
                        throws IOException, MalformedURLException, ParseException {
                Periodo periodo = new Periodo(startDate, endDate);

                String urlString = "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/CotacaoDolarPeriodo(dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)?%40dataInicial='"
                                + periodo.getDataInicial() + "'&%40dataFinalCotacao='" + periodo.getDataFinal()
                                + "'&%24format=json&%24skip=0&%24top=" + periodo.getDiasEntreAsDatasMaisUm();

                URL url = new URL(urlString);
                HttpURLConnection request = (HttpURLConnection) url.openConnection();
                request.connect();

                JsonElement response = JsonParser
                                .parseReader(new InputStreamReader((InputStream) request.getContent()));
                JsonObject rootObj = response.getAsJsonObject();
                JsonArray cotacoesArray = rootObj.getAsJsonArray("value");

                List<Moeda> moedasLista = new ArrayList<Moeda>();

                for (JsonElement obj : cotacoesArray) {
                        Moeda moedaRef = new Moeda();
                        Date data = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                        .parse(obj.getAsJsonObject().get("dataHoraCotacao").getAsString());

                        moedaRef.preco = obj.getAsJsonObject().get("cotacaoCompra").getAsDouble();
                        moedaRef.data = new SimpleDateFormat("dd/MM/yyyy").format(data);
                        moedaRef.hora = new SimpleDateFormat("HH:mm:ss").format(data);
                        moedasLista.add(moedaRef);
                }
                return moedasLista;
        }

        private JsonArray getCotacaoDolarDia(String dataCotacao) throws IOException, MalformedURLException {
                String urlString = "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/CotacaoDolarDia(dataCotacao=@dataCotacao)?@dataCotacao='"
                                + dataCotacao + "'&$top=100&$format=json";
                URL url = new URL(urlString);
                HttpURLConnection request = (HttpURLConnection) url.openConnection();
                request.connect();

                JsonElement response = JsonParser
                                .parseReader(new InputStreamReader((InputStream) request.getContent()));
                JsonObject rootObj = response.getAsJsonObject();
                JsonArray cotacoesArray = rootObj.getAsJsonArray("value");
                return cotacoesArray;
        }

        private Moeda formatCotacao(JsonObject cotacaoObj) throws ParseException {
                Moeda cotacao = new Moeda();
                Date timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                .parse(cotacaoObj.getAsJsonObject().get("dataHoraCotacao").getAsString());

                cotacao.preco = cotacaoObj.getAsJsonObject().get("cotacaoCompra").getAsDouble();
                cotacao.data = new SimpleDateFormat("dd/MM/yyyy").format(timestamp);
                cotacao.hora = new SimpleDateFormat("HH:mm:ss").format(timestamp);

                return cotacao;
        }

        public Moeda getCotacaoAtual() throws IOException, MalformedURLException, ParseException {
                LocalDate now = LocalDate.now();
                // A API do BCB espera a data no formato "MM-dd-yyyy"
                String nowFormatted = now.format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));

                JsonArray cotacoesArray = getCotacaoDolarDia(nowFormatted);

                if (cotacoesArray.size() > 0) {
                        JsonObject cotacaoObj = cotacoesArray.get(0).getAsJsonObject();

                        Moeda cotacaoAtual = formatCotacao(cotacaoObj);
                        return cotacaoAtual;
                } else {
                        // Se não houver cotação para o dia atual, tenta pegar a cotação do dia anterior
                        LocalDate yesterday = now.minusDays(1);
                        String yesterdayFormatted = yesterday.format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));

                        JsonArray cotacoesArrayYesterday = getCotacaoDolarDia(yesterdayFormatted);

                        JsonObject cotacaoObj = cotacoesArrayYesterday.get(0).getAsJsonObject();

                        Moeda cotacaoYesterday = formatCotacao(cotacaoObj);
                        return cotacaoYesterday;
                }
        }

        public List<Moeda> getCotacoesMenoresAtual(String startDate, String endDate)
                        throws IOException, MalformedURLException, ParseException {
                List<Moeda> cotacoesPeriodo = getCotacoesPeriodo(startDate, endDate);
                Moeda cotacaoAtual = getCotacaoAtual();
                List<Moeda> cotacoesMenores = cotacoesPeriodo.stream().filter(moeda -> moeda.preco < cotacaoAtual.preco)
                                .toList();
                return cotacoesMenores;
        }
}
