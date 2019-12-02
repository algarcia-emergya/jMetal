package org.uma.jmetal.algorithm.multiobjective.nsgaii;

import org.uma.jmetal.algorithm.impl.AbstractEvolutionaryAlgorithm;
import org.uma.jmetal.algorithm.impl.AbstractGeneticAlgorithm;
import org.uma.jmetal.component.densityestimator.DensityEstimator;
import org.uma.jmetal.component.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.component.ranking.Ranking;
import org.uma.jmetal.component.ranking.impl.FastNonDominatedSortRanking;
import org.uma.jmetal.component.replacement.Replacement;
import org.uma.jmetal.component.replacement.impl.RankingAndDensityEstimatorReplacement;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.component.termination.Termination;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.comparator.MultiComparator;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.observable.Observable;
import org.uma.jmetal.util.observable.ObservableEntity;
import org.uma.jmetal.util.observable.impl.DefaultObservable;

import java.util.*;

/** @author Antonio J. Nebro <antonio@lcc.uma.es> */
public class NSGAII<S extends Solution<?>> extends AbstractEvolutionaryAlgorithm<S, List<S>>
    implements ObservableEntity {
  private int evaluations;
  private int populationSize ;
  private int offspringPopulationSize;
  private int matingPoolSize;

  protected SelectionOperator<List<S>, S> selectionOperator ;
  protected CrossoverOperator<S> crossoverOperator ;
  protected MutationOperator<S> mutationOperator ;

  private final SolutionListEvaluator<S> evaluator;

  private Map<String, Object> algorithmStatusData;

  private Termination termination;
  private Replacement<S> replacement;

  private long startTime;
  private long totalComputingTime;

  private Observable<Map<String, Object>> observable;

  /** Constructor */
  public NSGAII(
      Problem<S> problem,
      int populationSize,
      int offspringPopulationSize,
      CrossoverOperator<S> crossoverOperator,
      MutationOperator<S> mutationOperator,
      Termination termination,
      Ranking<S> ranking,
      SolutionListEvaluator<S> evaluator) {

    this.populationSize = populationSize ;
    this.problem = problem ;

    this.crossoverOperator = crossoverOperator;
    this.mutationOperator = mutationOperator;

    DensityEstimator<S> densityEstimator = new CrowdingDistanceDensityEstimator<>();

    this.replacement =
        new RankingAndDensityEstimatorReplacement<>(
            ranking, densityEstimator, Replacement.RemovalPolicy.oneShot);

    this.selectionOperator =
        new BinaryTournamentSelection<>(
            new MultiComparator<>(
                Arrays.asList(
                    ranking.getSolutionComparator(), densityEstimator.getSolutionComparator())));

    this.termination = termination;

    this.evaluator = evaluator;
    this.offspringPopulationSize = offspringPopulationSize;

    this.algorithmStatusData = new HashMap<>();

    this.observable = new DefaultObservable<>("NSGAII algorithm");

    this.matingPoolSize = computeMatingPoolSize(offspringPopulationSize, crossoverOperator);
  }

  /** Constructor */
  public NSGAII(
      Problem<S> problem,
      int populationSize,
      int offspringPopulationSize,
      CrossoverOperator<S> crossoverOperator,
      MutationOperator<S> mutationOperator,
      Termination termination,
      SolutionListEvaluator<S> evaluator) {
    this(
        problem,
        populationSize,
        offspringPopulationSize,
        crossoverOperator,
        mutationOperator,
        termination,
        new FastNonDominatedSortRanking<>(),
        evaluator);
  }

  @Override
  public void run() {
    startTime = System.currentTimeMillis();
    super.run();
    totalComputingTime = System.currentTimeMillis() - startTime;
  }

  @Override
  protected void initProgress() {
    evaluations = populationSize;

    algorithmStatusData.put("EVALUATIONS", evaluations);
    algorithmStatusData.put("POPULATION", population);
    algorithmStatusData.put("COMPUTING_TIME", System.currentTimeMillis() - startTime);
  }

  @Override
  protected void updateProgress() {
    evaluations += offspringPopulationSize;
    algorithmStatusData.put("EVALUATIONS", evaluations);
    algorithmStatusData.put("POPULATION", population);
    algorithmStatusData.put("COMPUTING_TIME", System.currentTimeMillis() - startTime);

    observable.setChanged();
    observable.notifyObservers(algorithmStatusData);
  }

  @Override
  protected boolean isStoppingConditionReached() {
    return termination.isMet(algorithmStatusData);
  }

  @Override
  protected List<S> createInitialPopulation() {
    return null;
  }

  @Override
  protected List<S> evaluatePopulation(List<S> population) {
    population = evaluator.evaluate(population, getProblem());

    return population;
  }

  /**
   * This method iteratively applies a {@link SelectionOperator} to the population to fill the
   * mating pool population.
   *
   * @param population
   * @return The mating pool population
   */
  @Override
  protected List<S> selection(List<S> population) {
    List<S> matingPopulation = new ArrayList<>(population.size());
    for (int i = 0; i < matingPoolSize; i++) {
      S solution = selectionOperator.execute(population);
      matingPopulation.add(solution);
    }

    return matingPopulation;
  }

  /**
   * This methods iteratively applies a {@link CrossoverOperator} a {@link MutationOperator} to the
   * population to create the offspring population. The population size must be divisible by the
   * number of parents required by the {@link CrossoverOperator}; this way, the needed parents are
   * taken sequentially from the population.
   *
   * <p>The number of solutions returned by the {@link CrossoverOperator} must be equal to the
   * offspringPopulationSize state variable
   *
   * @param matingPool
   * @return The new created offspring population
   */
  @Override
  protected List<S> reproduction(List<S> matingPool) {
    int numberOfParents = crossoverOperator.getNumberOfRequiredParents();

    checkNumberOfParents(matingPool, numberOfParents);

    List<S> offspringPopulation = new ArrayList<>(offspringPopulationSize);
    for (int i = 0; i < matingPool.size(); i += numberOfParents) {
      List<S> parents = new ArrayList<>(numberOfParents);
      for (int j = 0; j < numberOfParents; j++) {
        parents.add(population.get(i + j));
      }

      List<S> offspring = crossoverOperator.execute(parents);

      for (S s : offspring) {
        mutationOperator.execute(s);
        offspringPopulation.add(s);
        if (offspringPopulation.size() >= offspringPopulationSize) break;
      }
    }
    return offspringPopulation;
  }

  @Override
  protected List<S> replacement(List<S> population, List<S> offspringPopulation) {
    List<S> newPopulation = replacement.replace(population, offspringPopulation);

    return newPopulation;
  }

  @Override
  public List<S> getResult() {
    return SolutionListUtils.getNonDominatedSolutions(getPopulation());
  }

  @Override
  public String getName() {
    return "NSGAII";
  }

  @Override
  public String getDescription() {
    return "Nondominated Sorting Genetic Algorithm version II";
  }

  public Map<String, Object> getAlgorithmStatusData() {
    return algorithmStatusData;
  }

  @Override
  public Observable<Map<String, Object>> getObservable() {
    return observable;
  }

  public long getTotalComputingTime() {
    return totalComputingTime;
  }

  public long getEvaluations() {
    return evaluations;
  }

  protected int computeMatingPoolSize(
      int offspringPopulationSize, CrossoverOperator<S> crossoverOperator) {
    int size =
        offspringPopulationSize
            * crossoverOperator.getNumberOfRequiredParents()
            / crossoverOperator.getNumberOfGeneratedChildren();
    int remainder = size % crossoverOperator.getNumberOfRequiredParents();
    if (remainder != 0) {
      size += remainder;
    }

    return size;
  }
}
