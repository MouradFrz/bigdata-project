import React, { useMemo } from 'react';
import { useGetTopRatedQuery } from '../../../store/services/stats';
import ReactApexChart from 'react-apexcharts';
import { IRootState } from '../../../store';

import { useSelector } from 'react-redux';
import Loader from '../../../components/Loader';
import { MetricSchema } from '../../../types';

function TopRatedHistogramme({ data: metricData }: { data: MetricSchema }) {
    const isDark = useSelector((state: IRootState) => state.themeConfig.theme === 'dark' || state.themeConfig.isDarkMode);
    const { data, isError, isLoading } = useGetTopRatedQuery();
    const simpleColumnStacked: any = useMemo(() => {
        if (!data) return null;
        return {
            series: [
                {
                    name: "Nombre d'achat",
                    data: data.map((product) => product.ratingNumber),
                },
                {
                    name: 'Note moyenne',
                    data: data.map((product) => product.averageRating.toFixed(3)),
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
                        horizontal: false,
                    },
                },
                xaxis: {
                    type: 'string',
                    categories: data.map((product) => product.title),
                    axisBorder: {
                        color: isDark ? '#191e3a' : '#e0e6ed',
                    },
                },
                yaxis: {
                    opposite: false,
                    labels: {
                        offsetX: 0,
                        show: false,
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
            <ReactApexChart series={simpleColumnStacked.series} options={simpleColumnStacked.options} className="rounded-lg bg-white dark:bg-black overflow-hidden" type="bar" height={600} />;
        </>
    );
}

export default TopRatedHistogramme;
