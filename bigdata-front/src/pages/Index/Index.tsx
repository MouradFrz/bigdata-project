import SearchBar from '../../components/Index/SearchBar';
import SearchResults from '../../components/Index/SearchResults';
import Loader from '../../components/Loader';
import useIndexVM from './indexVM';
import Tippy from '@tippyjs/react';
import 'tippy.js/dist/tippy.css';
const Index = () => {
    const { incrementCurrentPage, decrementCurrentPage, loading, error, displayedProducts, currentPage, searchKeyword, setSearchKeyword } = useIndexVM();
    if (error && 'status' in error && error.status === 400)
        return (
            <div className="container">
                <SearchBar searchKeyword={searchKeyword} setSearchKeyword={setSearchKeyword} />
                <p>Invalid Request</p>
            </div>
        );
    if (error) return <>Something went wrong</>;
    return (
        <div className="container">
            <SearchBar searchKeyword={searchKeyword} setSearchKeyword={setSearchKeyword} />
            <Tippy
                content={
                    <>
                        <p>
                            Vous pouvez écrire des requetes qui contiennent des conditions sur le nom d'un produit ou d'une categorie, le prix, etc...Vous pouvez écrire des requetes qui contiennent
                            des conditions sur le nom d'un produit ou d'une categorie, le prix, etc...Vous pouvez écrire des requetes qui contiennent des conditions sur le nom d'un produit ou d'une
                            categorie, le prix, etc...Vous pouvez écrire des requetes qui contiennent des conditions sur le nom d'un produit ou d'une categorie, le prix, la note, un mot clé etc...
                        </p>
                        <hr />
                        <h2 className="font-extrabold">Examples de requetes:</h2>
                        <ul className="list-disc ms-6">
                            <li>Donne-moi les produits qui ont un prix inférieur à 100.</li>
                            <li>Montre-moi les produits dont la catégorie principale est électronique.</li>
                            <li>Je cherche des produits avec un prix inférieur à 50 et supérieur a 10 ainsi qu'une note supérieure à 4.</li>
                            <li>Je veux voir les produits dans la catégorie vêtements qui contiennent le mot “jean”.</li>
                            <li>Quels sont les produits dont la note est supérieure à 4 ainsi qu'un prix égal à 200 ?</li>
                        </ul>
                    </>
                }
                maxWidth={900}
                placement={'top-start'}
            >
                <p className="font-extrabold">Comment formuler ma requete ?</p>
            </Tippy>
            <h1 className="font-bold text-xl my-4">Products</h1>
            {loading ? (
                <div className="flex justify-center">
                    <Loader />{' '}
                </div>
            ) : (
                displayedProducts && <SearchResults products={displayedProducts} />
            )}
            <div className="flex">
                <button className="btn rounded-full" onClick={decrementCurrentPage}>
                    {' '}
                    &lt;
                </button>
                <button className="btn btn-primary rounded-full">{currentPage + 1}</button>
                <button className="btn rounded-full" onClick={incrementCurrentPage}>
                    {' '}
                    &gt;
                </button>
            </div>
        </div>
    );
};

export default Index;
