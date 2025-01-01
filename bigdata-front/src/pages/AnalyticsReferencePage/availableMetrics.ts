import { MetricSchema } from '../../types';
import TopRatedHistogramme from '../Metric/Metrics/TopRatedHistogramme';

export const AVAILABLE_METRICS: MetricSchema[] = [
    {
        id: 1,
        title: 'Top rated products',
        description: 'Un histogram qui represente les 10 produits les mieux note dans le systeme et avec un nombre minimum de review de 60000',
        component: TopRatedHistogramme,
    },
    // {
    //     type: 'memory_usage',
    //     dataUrl: '/api/metrics/memory',
    //     id: 2,
    //     title: 'Memory Usage',
    //     description: 'Tracks the memory consumption of the system.',
    // },
    // {
    //     type: 'disk_io',
    //     dataUrl: '/api/metrics/disk-io',
    //     id: 3,
    //     title: 'Disk I/O',
    //     description: 'Measures the input and output operations on the disk.',
    // },
    // {
    //     type: 'network_latency',
    //     dataUrl: '/api/metrics/network-latency',
    //     id: 4,
    //     title: 'Network Latency',
    //     description: 'Calculates the delay in network communication.',
    // },
    // {
    //     type: 'http_requests',
    //     dataUrl: '/api/metrics/http-requests',
    //     id: 5,
    //     title: 'HTTP Requests',
    //     description: 'Counts the number of HTTP requests handled by the server.',
    // },
    // {
    //     type: 'database_queries',
    //     dataUrl: '/api/metrics/database-queries',
    //     id: 6,
    //     title: 'Database Queries',
    //     description: 'Tracks the number and speed of database queries.',
    // },
    // {
    //     type: 'uptime',
    //     dataUrl: '/api/metrics/uptime',
    //     id: 7,
    //     title: 'System Uptime',
    //     description: 'Reports how long the system has been running without interruption.',
    // },
    // {
    //     type: 'error_rate',
    //     dataUrl: '/api/metrics/error-rate',
    //     id: 8,
    //     title: 'Error Rate',
    //     description: 'Monitors the percentage of failed operations.',
    // },
    // {
    //     type: 'temperature',
    //     dataUrl: '/api/metrics/temperature',
    //     id: 9,
    //     title: 'System Temperature',
    //     description: "Measures the temperature of the system's hardware components.",
    // },
    // {
    //     type: 'cache_hits',
    //     dataUrl: '/api/metrics/cache-hits',
    //     id: 10,
    //     title: 'Cache Hits',
    //     description: 'Tracks how often requested data is found in the cache.',
    // },
];
