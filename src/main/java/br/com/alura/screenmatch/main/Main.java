package br.com.alura.screenmatch.main;

import br.com.alura.screenmatch.model.DadosSerie;
import br.com.alura.screenmatch.model.DadosTemporada;
import br.com.alura.screenmatch.model.Episodio;
import br.com.alura.screenmatch.model.Serie;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConversorDados;

import java.util.*;
import java.util.stream.Collectors;

public class Main {
    private Scanner leitura = new Scanner(System.in);
    private final String URL = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=7b75c184";
    private ConsumoApi consumoApi = new ConsumoApi();
    private ConversorDados conversorDados = new ConversorDados();
    private SerieRepository serieRepository;
    private List<Serie> series = new ArrayList<>();

    public Main(SerieRepository serieRepository) {
        this.serieRepository = serieRepository;
    }

    public void showMenu() {
        var opcao = 0;

        do {
            var menu = """
                    \n1 - Buscar séries
                    2 - Buscar episódios      
                    3 - Listar as séries buscadas        
                    0 - Sair   
                    
                    Digite a opção desejada:""";

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 0:
                    System.out.println("Encerrando aplicação");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        } while (opcao != 0);
    }

    private void buscarSerieWeb() {
        DadosSerie dadosSerie = getDadosSerie();
        Serie serie = new Serie(dadosSerie);

        serieRepository.save(serie);

        System.out.println(serie);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para buscar:");
        var nomeSerie = leitura.nextLine();

        System.out.println("Buscando a série. Aguarde um momento...");
        var json = consumoApi.pegarDados(URL + nomeSerie.replace(" ", "+") + API_KEY);

        return conversorDados.pegarDados(json, DadosSerie.class);
    }

    private void buscarEpisodioPorSerie(){
        listarSeriesBuscadas();
        System.out.println("Digite o nome da serie:");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> optionalSerie = series.stream()
                .filter(s -> s.getTitulo().toLowerCase().contains(nomeSerie.toLowerCase()))
                .findFirst();

        if(optionalSerie.isPresent()) {
            var serie = optionalSerie.get();

            List<DadosTemporada> temporadas = new ArrayList<>();

            for(int i = 1; i <= serie.getTotalTemporadas(); i++) {
                var json = consumoApi.pegarDados(URL + serie.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);

                var temporada = conversorDados.pegarDados(json, DadosTemporada.class);

                temporadas.add(temporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(t -> t.dadosEpisodios().stream()
                            .map(e -> new Episodio(t.numero(), e)))
                    .collect(Collectors.toList());

            serie.setEpisodios(episodios);

            serieRepository.save(serie);
        }
        else {
            System.out.println("Série não encontrada");
        }
    }

    private void listarSeriesBuscadas() {
        series = serieRepository.findAll();

        series.stream()
                .sorted(Comparator.comparing(Serie::getTitulo))
                .forEach(System.out::println);
    }
}