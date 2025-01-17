import { MetricSchema } from '../../types';
import TopRatedHistogramme from '../Metric/Metrics/TopRatedHistogramme';

export const AVAILABLE_METRICS: MetricSchema[] = [
    {
        id: 1,
        title: 'Top rated products',
        description: 'Un histogramme qui represente les 10 produits les mieux note dans le systeme et avec un nombre minimum de review de 60000',
        component: TopRatedHistogramme,
    },
];
