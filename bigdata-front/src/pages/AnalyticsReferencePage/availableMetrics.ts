import { MetricSchema } from '../../types';
import RatingDistributionHistogramme from '../Metric/Metrics/RatingDistributionHistogramme';
import TopRatedHistogramme from '../Metric/Metrics/TopRatedHistogramme';

export const AVAILABLE_METRICS: MetricSchema[] = [
    {
        id: 1,
        title: 'Top rated products',
        description: 'Un histogramme qui represente les 10 produits les mieux note dans le systeme et avec un nombre minimum de review de 60000',
        component: TopRatedHistogramme,
    },
    {
        id: 2,
        title: 'Rating distribution',
        description:
            "Un histogramme qui représente la distribution des notes moyennes (de 1 à 5 étoiles) de tous les produits dans le système, permettant de visualiser le nombre de produits pour chaque niveau de notation et d'identifier les tendances générales de satisfaction client.",
        component: RatingDistributionHistogramme,
    },
];
