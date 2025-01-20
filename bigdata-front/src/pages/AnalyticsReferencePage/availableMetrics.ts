import { MetricSchema } from '../../types';
import RatingDistributionHistogramme from '../Metric/Metrics/RatingDistributionHistogramme';
import ReviewTimeline from '../Metric/Metrics/ReviewTimeline';
import TopRatedHistogramme from '../Metric/Metrics/TopRatedHistogramme';
import VerifiedReviewComparison from '../Metric/Metrics/VerifiedReviewComparison';

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
    {
        id: 3,
        title: 'Review timeline',
        description: 'Un graphique qui représente le nombre de reviews par mois pour tous les produits dans le système, permettant de visualiser les tendances de reviews dans le temps.',
        component: ReviewTimeline,
    },
    {
        id: 4,
        title: 'Verified vs Non-Verified Reviews',
        description:
            "Un graphique comparatif montrant l'évolution des notes moyennes et du nombre de reviews entre les achats vérifiés et non vérifiés, permettant d'analyser les différences de comportement entre ces deux types d'évaluations.",
        component: VerifiedReviewComparison,
    },
];
