import { useState } from 'react';

function SearchBar({ searchKeyword, setSearchKeyword }: { searchKeyword: string; setSearchKeyword: (arg0: string) => void }) {
    const [focus, setFocus] = useState(false);

    const overlaySearchClick = () => {
        setFocus(true);
    };

    return (
        <form>
            <div className="relative border border-white-dark/20  w-full flex">
                <button type="submit" placeholder="Let's find your question in fast way" className="text-primary m-auto p-3 flex items-center justify-center">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <circle cx="11.5" cy="11.5" r="9.5" stroke="currentColor" strokeWidth="1.5" opacity="0.5"></circle>
                        <path d="M18.5 18.5L22 22" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"></path>
                    </svg>
                </button>
                <input
                    type="text"
                    placeholder="Let's find your question in fast way"
                    value={searchKeyword}
                    onChange={(ev) => {
                        setSearchKeyword(ev.target.value);
                    }}
                    className="form-input border-0 border-l rounded-none bg-white  focus:shadow-[0_0_5px_2px_rgb(194_213_255_/_62%)] dark:shadow-[#1b2e4b] placeholder:tracking-wider focus:outline-none py-3"
                />
            </div>
        </form>
    );
}

export default SearchBar;
