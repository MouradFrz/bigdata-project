import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { AVAILABLE_METRICS } from '../AnalyticsReferencePage/availableMetrics';
import { MetricSchema } from '../../types';

function Metric() {
    const { metricId } = useParams<{ metricId: string }>();
    const [isInvalidMetric, setIsInvalidMetric] = useState<boolean>(false);
    const [metricData, setMetricData] = useState<MetricSchema | null>(null);
    useEffect(() => {
        if (!AVAILABLE_METRICS.map((metric) => metric.id).includes(Number(metricId))) {
            setIsInvalidMetric(true);
        } else {
            setMetricData(AVAILABLE_METRICS.find((metric) => metric.id === Number(metricId)) as MetricSchema);
        }
    }, []);
    if (isInvalidMetric) {
        return <>The metric you are trying to access is non existant</>;
    }
    if (metricData === null) return <div>Something went wrong</div>;
    return <metricData.component data={metricData} />;
}

export default Metric;
