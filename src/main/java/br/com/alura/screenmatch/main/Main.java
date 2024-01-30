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
                    \n1 - Buscar dados de série
                    2 - Buscar dados de episódios      
                    3 - Listar as séries buscadas
                    4 - Pesquisar série buscada 
                    5 - Buscar séries por ator       
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
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriePorAtor();
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

        Optional<Serie> optionalSerie = serieRepository.findByTituloContainingIgnoreCase(nomeSerie);

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

    private void buscarSeriePorTitulo() {
        System.out.println("Digite o nome da serie:");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> serieBuscada = serieRepository.findByTituloContainingIgnoreCase(nomeSerie);

        if(serieBuscada.isPresent()) {
            System.out.printf("Serie encontrada:");
            System.out.println(serieBuscada.get());
        }
        else {
            System.out.println("Serie não encontrada");
        }
    }

    private void buscarSeriePorAtor() {
        System.out.println("Digite o nome do ator ou da atriz:");
        var nomeAtor = leitura.nextLine();
        System.out.println("Avaliação mínima da série:");
        var avaliacao = leitura.nextDouble();

        List<Serie> seriesEncontradas = (avaliacao > 0)
                ? serieRepository.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao)
                : serieRepository.findByAtoresContainingIgnoreCase(nomeAtor);

        if(!seriesEncontradas.isEmpty()) {
            System.out.println((seriesEncontradas.size() > 1) ? "Series encontradas:" : "Serie encontrada:");
            seriesEncontradas.forEach(System.out::println);
        }
        else {
            System.out.println("Nenhuma série encontrada");
        }
    }
}