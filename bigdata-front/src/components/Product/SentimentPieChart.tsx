import { ApexOptions } from 'apexcharts';
import React from 'react';
import ReactApexChart from 'react-apexcharts';
import { useGetSentimentQuery } from '../../store/services/stats';
import Loader from '../Loader';

function SentimentPieChart({ parentAsin }: { parentAsin: string }) {
    const { data, isLoading } = useGetSentimentQuery({ parentAsin });
    const options = {
        chart: {
            type: 'pie',
        },
        labels: ['Positif', 'Neutre', 'Negatif'],
        colors: ['#008FFB', '#00E396', '#FEB019'],
        legend: {
            position: 'bottom',
        },
        dataLabels: {
            enabled: true,
            formatter: 22,
        },
        tooltip: {
            y: {
                formatter: 6,
            },
        },
    };

    const series = [data?.positiveCount, data?.neutralCount, data?.negativeCount]; // Values for each category
    if (isLoading) return <Loader />;
    return <ReactApexChart options={options as unknown as ApexOptions} series={series as unknown as ApexAxisChartSeries} type="pie" height={350} />;
}

export default SentimentPieChart;
