import React, { useMemo } from 'react';
import { useSelector } from 'react-redux';
import { IRootState } from '../../../store';
import Loader from '../../../components/Loader';
import { MetricSchema } from '../../../types';
import { useGetRatingDistributionQuery } from '../../../store/services/stats';
import ReactApexChart from 'react-apexcharts';

function RatingDistributionHistogramme({ data: metricData }: { data: MetricSchema }) {
    const isDark = useSelector((state: IRootState) => state.themeConfig.theme === 'dark' || state.themeConfig.isDarkMode);
    const { data, isError, isLoading } = useGetRatingDistributionQuery();
    const simpleColumnStacked: any = useMemo(() => {
        if (!data) return null;
        return {
            series: [
                {
                    name: 'Nombre de review',
                    data: data.map((note) => note.count),
                },
            ],
            options: {
                chart: {
                    height: 600,
                    type: 'bar',
                    stacked: true,
                    zoom: {
                        enabled: true,
                    },
                    toolbar: {
                        show: true,
                    },
                },
                colors: ['#2196f3', '#3b3f5c'],
                responsive: [
                    {
                        breakpoint: 480,
                        options: {
                            legend: {
                                position: 'bottom',
                                offsetX: -10,
                                offsetY: 5,
                            },
                        },
                    },
                ],
                plotOptions: {
                    bar: {
                        horizontal: true,
                    },
                },
                xaxis: {
                    type: 'float',
                    categories: data.map((product) => product.rating),
                    axisBorder: {
                        color: isDark ? '#191e3a' : '#e0e6ed',
                    },
                },
                yaxis: {
                    opposite: false,
                    labels: {
                        offsetX: 0,
                    },
                },
                grid: {
                    borderColor: isDark ? '#191e3a' : '#e0e6ed',
                },
                legend: {
                    position: 'right',
                    offsetY: 40,
                },
                tooltip: {
                    theme: isDark ? 'dark' : 'light',
                },
                fill: {
                    opacity: 0.8,
                },
            },
        };
    }, [data]);
    if (isError) return <>Something went wrong</>;
    if (isLoading)
        return (
            <div className="flex justify-center">
                <Loader />{' '}
            </div>
        );
    return (
        <>
            <h1 className="font-extrabold text-3xl">{metricData.title}</h1>
            <p className="my-2">{metricData.description}</p>
            <ReactApexChart series={simpleColumnStacked.series} options={simpleColumnStacked.options} className="rounded-lg bg-white dark:bg-black overflow-hidden" type="bar" height={800} />;
        </>
    );
}

export default RatingDistributionHistogramme;
